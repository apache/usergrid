/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.mongo.protocol;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.mongo.MongoChannelHandler;
import org.apache.usergrid.mongo.commands.MongoCommand;
import org.apache.usergrid.mongo.query.MongoQueryParser;
import org.apache.usergrid.mongo.utils.BSONUtils;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.security.shiro.PrincipalCredentialsToken;
import org.apache.usergrid.security.shiro.utils.SubjectUtils;
import org.apache.usergrid.utils.MapUtils;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.subject.Subject;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.persistence.index.query.Query.Level;

import static org.apache.usergrid.utils.JsonUtils.toJsonMap;
import static org.apache.usergrid.utils.MapUtils.entry;
import static org.apache.usergrid.utils.MapUtils.map;


public class OpQuery extends OpCrud {

    private static final Logger logger = LoggerFactory.getLogger( OpQuery.class );

    int flags;
    int numberToSkip;
    int numberToReturn;
    BSONObject query;
    BSONObject returnFieldSelector;

    static Set<String> operators = new HashSet<String>();


    static {
        operators.add( "all" );
        operators.add( "and" );
        operators.add( "elemMatch" );
        operators.add( "exists" );
        operators.add( "gt" );
        operators.add( "gte" );
        operators.add( "in" );
        operators.add( "lt" );
        operators.add( "lte" );
        operators.add( "mod" );
        operators.add( "ne" );
        operators.add( "nin" );
        operators.add( "nor" );
        operators.add( "not" );
        operators.add( "or" );
        operators.add( "regex" );
        operators.add( "size" );
        operators.add( "type" );
        operators.add( "where" );
    }


    public OpQuery() {
        opCode = OP_QUERY;
    }


    public int getFlags() {
        return flags;
    }


    public void setFlags( int flags ) {
        this.flags = flags;
    }


    public int getNumberToSkip() {
        return numberToSkip;
    }


    public void setNumberToSkip( int numberToSkip ) {
        this.numberToSkip = numberToSkip;
    }


    public int getNumberToReturn() {
        return numberToReturn;
    }


    public void setNumberToReturn( int numberToReturn ) {
        this.numberToReturn = numberToReturn;
    }


    public BSONObject getQuery() {
        return query;
    }


    public void setQuery( BSONObject query ) {
        this.query = query;
    }


    public void setQuery( Map<?, ?> map ) {
        query = new BasicBSONObject();
        query.putAll( map );
    }


    public BSONObject getReturnFieldSelector() {
        return returnFieldSelector;
    }


    public void setReturnFieldSelector( BSONObject returnFieldSelector ) {
        this.returnFieldSelector = returnFieldSelector;
    }


    public void setReturnFieldSelector( Map<?, ?> map ) {
        returnFieldSelector = new BasicBSONObject();
        returnFieldSelector.putAll( map );
    }


    @Override
    public void decode( ChannelBuffer buffer ) throws IOException {
        super.decode( buffer );
        flags = buffer.readInt();
        fullCollectionName = readCString( buffer );
        numberToSkip = buffer.readInt();
        numberToReturn = buffer.readInt();
        query = BSONUtils.decoder().readObject( new ChannelBufferInputStream( buffer ) );
        if ( buffer.readable() ) {
            returnFieldSelector = BSONUtils.decoder().readObject( new ChannelBufferInputStream( buffer ) );
            logger.info( "found fieldSeclector: {}", returnFieldSelector );
        }
    }


    @Override
    public ChannelBuffer encode( ChannelBuffer buffer ) {
        int l = 28; // 7 ints * 4 bytes

        ByteBuffer fullCollectionNameBytes = getCString( fullCollectionName );
        l += fullCollectionNameBytes.capacity();

        ByteBuffer queryBytes = encodeDocument( query );
        l += queryBytes.capacity();

        ByteBuffer returnFieldSelectorBytes = encodeDocument( returnFieldSelector );
        l += returnFieldSelectorBytes.capacity();

        messageLength = l;

        buffer = super.encode( buffer );

        buffer.writeInt( flags );

        buffer.writeBytes( fullCollectionNameBytes );

        buffer.writeInt( numberToSkip );
        buffer.writeInt( numberToReturn );

        buffer.writeBytes( queryBytes );

        buffer.writeBytes( returnFieldSelectorBytes );

        return buffer;
    }


    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.mongo.protocol.OpCrud#doOp()
     */
    @Override
    public OpReply doOp( MongoChannelHandler handler, ChannelHandlerContext ctx, MessageEvent messageEvent ) {
        logger.debug( "In OpQuery.doOp with fullCollectionName: {}", fullCollectionName );
        Subject currentUser = SubjectUtils.getSubject();

        String collectionName = getCollectionName();

        if ( "$cmd".equals( collectionName ) ) {

            @SuppressWarnings("unchecked") String commandName = ( String ) MapUtils.getFirstKey( getQuery().toMap() );

            if ( "authenticate".equals( commandName ) ) {
                return handleAuthenticate( handler, getDatabaseName() );
            }

            if ( "getnonce".equals( commandName ) ) {
                return handleGetnonce();
            }

            if ( !currentUser.isAuthenticated() ) {
                return handleUnauthorizedCommand( messageEvent );
            }

            MongoCommand command = MongoCommand.getCommand( commandName );

            if ( command != null ) {
                logger.info( "found command {} from name {}", command.getClass().getName(), commandName );
                return command.execute( handler, ctx, messageEvent, this );
            }
            else {
                logger.info( "No command for " + commandName );
            }
        }

        if ( !currentUser.isAuthenticated() ) {
            return handleUnauthorizedQuery( messageEvent );
        }

        if ( "system.namespaces".equals( collectionName ) ) {
            return handleListCollections( handler, getDatabaseName() );
        }

        if ( "system.users".equals( collectionName ) ) {
            return handleListUsers();
        }

        return handleQuery( handler );
    }


    private OpReply handleAuthenticate( MongoChannelHandler handler, String databaseName ) {
        logger.info( "Authenticating for database " + databaseName + "... " );
        String name = ( String ) query.get( "user" );
        String nonce = ( String ) query.get( "nonce" );
        String key = ( String ) query.get( "key" );

        UserInfo user = null;
        try {
            user = handler.getOrganizations().verifyMongoCredentials( name, nonce, key );
        }
        catch ( Exception e1 ) {
            return handleAuthFails( this );
        }
        if ( user == null ) {
            return handleAuthFails( this );
        }

        PrincipalCredentialsToken token =
                PrincipalCredentialsToken.getFromAdminUserInfoAndPassword(
                        user, key, handler.getEmf().getManagementAppId() );
        Subject subject = SubjectUtils.getSubject();

        try {
            subject.login( token );
        }
        catch ( AuthenticationException e2 ) {
            return handleAuthFails( this );
        }

        OpReply reply = new OpReply( this );
        reply.addDocument( map( "ok", 1.0 ) );
        return reply;
    }


    private OpReply handleGetnonce() {
        String nonce = String.format( "%04x", ( new Random() ).nextLong() );
        OpReply reply = new OpReply( this );
        reply.addDocument( map( entry( "nonce", nonce ), entry( "ok", 1.0 ) ) );
        return reply;
    }


    private OpReply handleUnauthorizedCommand( MessageEvent e ) {
        // { "assertion" : "unauthorized db:admin lock type:-1 client:127.0.0.1"
        // , "assertionCode" : 10057 , "errmsg" : "db assertion failure" , "ok"
        // : 0.0}
        OpReply reply = new OpReply( this );
        reply.addDocument( map( entry( "assertion",
                "unauthorized db:" + getDatabaseName() + " lock type:-1 client:" + ( ( InetSocketAddress ) e
                        .getRemoteAddress() ).getAddress().getHostAddress() ), entry( "assertionCode", 10057 ),
                entry( "errmsg", "db assertion failure" ), entry( "ok", 0.0 ) ) );
        return reply;
    }


    private OpReply handleUnauthorizedQuery( MessageEvent e ) {
        // { "$err" : "unauthorized db:test lock type:-1 client:127.0.0.1" ,
        // "code" : 10057}
        OpReply reply = new OpReply( this );
        reply.addDocument( map( entry( "$err",
                "unauthorized db:" + getDatabaseName() + " lock type:-1 client:" + ( ( InetSocketAddress ) e
                        .getRemoteAddress() ).getAddress().getHostAddress() ), entry( "code", 10057 ) ) );
        return reply;
    }


    private OpReply handleAuthFails( OpQuery opQuery ) {
        // { "errmsg" : "auth fails" , "ok" : 0.0}
        OpReply reply = new OpReply( opQuery );
        reply.addDocument( map( entry( "errmsg", "auth fails" ), entry( "ok", 0.0 ) ) );
        return reply;
    }


    private OpReply handleListCollections( MongoChannelHandler handler, String databaseName ) {
        logger.info( "Handling list collections for database {} ... ", databaseName );
        Identifier id = Identifier.from( databaseName );

        OpReply reply = new OpReply( this );

        ApplicationInfo application = SubjectUtils.getApplication( id );

        if ( application == null ) {
            return reply;
        }

        EntityManager em = handler.getEmf().getEntityManager( application.getId() );


        try {
            Set<String> collections = em.getApplicationCollections();
            for ( String colName : collections ) {
                if ( Schema.isAssociatedEntityType( colName ) ) {
                    continue;
                }
                reply.addDocument( map( "name", String.format( "%s.%s", databaseName, colName ) ) );
                reply.addDocument( map( "name", String.format( "%s.%s.$_id_", databaseName, colName ) ) );
            }
            // reply.addDocument(map("name", databaseName + ".system.indexes"));
        }
        catch ( Exception ex ) {
            logger.error( "Unable to retrieve collections", ex );
        }
        return reply;
    }


    private OpReply handleListUsers() {
        logger.info( "Handling list users for database {} ...  ", getDatabaseName() );

        OpReply reply = new OpReply( this );
        return reply;
    }


    private OpReply handleQuery( MongoChannelHandler handler ) {
        logger.info( "Handling a query... " );
        OpReply reply = new OpReply( this );

        ApplicationInfo application = SubjectUtils.getApplication( Identifier.from( getDatabaseName() ) );
        if ( application == null ) {
            return reply;
        }

        int count = getNumberToReturn();
        if ( count <= 0 ) {
            count = 30;
        }

        EntityManager em = handler.getEmf().getEntityManager( application.getId() );

        try {
            Results results = null;
            Query q = MongoQueryParser.toNativeQuery( query, returnFieldSelector, numberToReturn );
            if ( q != null ) {
                results = em.searchCollection( em.getApplicationRef(), getCollectionName(), q );
            }
            else {
                results = em.getCollection( em.getApplicationRef(), getCollectionName(), null, count,
                        Level.ALL_PROPERTIES, false );
            }
            if ( !results.isEmpty() ) {
                for ( Entity entity : results.getEntities() ) {

                    Object savedId = entity.getProperty( "_id" );
                    Object mongoId = null;

                    //try to parse it into an ObjectId
                    if ( savedId == null ) {
                        mongoId = entity.getUuid();
                    }
                    else {
                        try {
                            mongoId = new ObjectId( savedId.toString() );
                            //it's not a mongo Id, use it as is
                        }
                        catch ( IllegalArgumentException iae ) {
                            mongoId = savedId;
                        }
                    }

                    reply.addDocument( map( entry( "_id", mongoId ), toJsonMap( entity ),
                            entry( Schema.PROPERTY_UUID, entity.getUuid().toString() ) ) );
                }
            }
        }
        catch ( Exception ex ) {
            logger.error( "Unable to retrieve collections", ex );
        }
        return reply;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "OpQuery [flags=" + flags + ", numberToSkip=" + numberToSkip + ", numberToReturn=" + numberToReturn
                + ", query=" + query + ", returnFieldSelector=" + returnFieldSelector + ", fullCollectionName="
                + fullCollectionName + ", messageLength=" + messageLength + ", requestID=" + requestID + ", responseTo="
                + responseTo + ", opCode=" + opCode + "]";
    }
}
