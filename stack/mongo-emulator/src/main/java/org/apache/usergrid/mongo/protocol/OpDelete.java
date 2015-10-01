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
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.mongo.MongoChannelHandler;
import org.apache.usergrid.mongo.query.MongoQueryParser;
import org.apache.usergrid.mongo.utils.BSONUtils;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.persistence.index.query.Query.Level;
import org.apache.usergrid.security.shiro.utils.SubjectUtils;


public class OpDelete extends OpCrud {

    private static final Logger logger = LoggerFactory.getLogger( OpDelete.class );
    public static final int BATCH_SIZE = 1000;

    private int flags;
    // delete query
    private BSONObject selector;


    public OpDelete() {
        opCode = OP_DELETE;
    }


    public int getFlags() {
        return flags;
    }


    public void setFlags( int flags ) {
        this.flags = flags;
    }


    public BSONObject getSelector() {
        return selector;
    }


    public void setSelector( BSONObject selector ) {
        this.selector = selector;
    }


    public void setSelector( Map<?, ?> map ) {
        selector = new BasicBSONObject();
        selector.putAll( map );
    }


    @Override
    public void decode( ChannelBuffer buffer ) throws IOException {
        super.decode( buffer );
        buffer.readInt();
        fullCollectionName = readCString( buffer );
        flags = buffer.readInt();
        selector = BSONUtils.decoder().readObject( new ChannelBufferInputStream( buffer ) );
    }


    @Override
    public ChannelBuffer encode( ChannelBuffer buffer ) {
        int l = 24; // 6 ints * 4 bytes

        ByteBuffer fullCollectionNameBytes = getCString( fullCollectionName );
        l += fullCollectionNameBytes.capacity();

        ByteBuffer selectorBytes = encodeDocument( selector );
        l += selectorBytes.capacity();

        messageLength = l;

        buffer = super.encode( buffer );

        buffer.writeInt( 0 );

        buffer.writeBytes( fullCollectionNameBytes );

        buffer.writeInt( flags );

        buffer.writeBytes( selectorBytes );

        return buffer;
    }


    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.mongo.protocol.OpCrud#doOp(org.apache.usergrid.mongo.
     * MongoChannelHandler, org.jboss.netty.channel.ChannelHandlerContext,
     * org.jboss.netty.channel.MessageEvent)
     */
    @Override
    public OpReply doOp( MongoChannelHandler handler, ChannelHandlerContext ctx, MessageEvent messageEvent ) {

        // perform the query
        Query query = MongoQueryParser.toNativeQuery( selector, 0 );

        // TODO TN set an error
        if ( query == null ) {
            return null;
        }

        query.setResultsLevel( Level.IDS );
        query.setLimit( BATCH_SIZE );

        ApplicationInfo application = SubjectUtils.getApplication( Identifier.from( getDatabaseName() ) );

        if ( application == null ) {
            ctx.setAttachment( new IllegalArgumentException(
                    String.format( "Could not find application with name '%s' ", getDatabaseName() ) ) );
            return null;
        }

        // delete every result

        EntityManager em = handler.getEmf().getEntityManager( application.getId() );

        Results results = null;

        do {

            try {

                if ( results != null ) {
                    query.setCursor( results.getCursor() );
                }

                results = em.searchCollection( em.getApplicationRef(), getCollectionName(), query );

                // now loop through all the ids and delete them
                for ( UUID id : results.getIds() ) {
                    em.delete( new SimpleEntityRef( query.getEntityType(), id ) );
                }
            }
            catch ( Exception ex ) {
                logger.error( "Unable to delete object", ex );
                ctx.setAttachment( ex );
            }
        }
        while ( results.getCursor() != null );

        // return nothing on delete, like insert

        return null;
    }


    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "OpDelete [flags=" + flags + ", selector=" + selector + ", fullCollectionName=" + fullCollectionName
                + ", messageLength=" + messageLength + ", requestID=" + requestID + ", responseTo=" + responseTo
                + ", opCode=" + opCode + "]";
    }
}
