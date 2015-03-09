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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import org.apache.usergrid.mongo.MongoChannelHandler;
import org.apache.usergrid.mongo.utils.BSONUtils;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.security.shiro.utils.SubjectUtils;


public class OpInsert extends OpCrud {

    private static final Logger logger = LoggerFactory.getLogger( OpInsert.class );

    protected int flags;
    protected List<BSONObject> documents = new ArrayList<BSONObject>();


    public OpInsert() {
        opCode = OP_INSERT;
    }


    public int getFlags() {
        return flags;
    }


    public void setFlags( int flags ) {
        this.flags = flags;
    }


    public List<BSONObject> getDocuments() {
        return documents;
    }


    public void setDocuments( List<BSONObject> documents ) {
        if ( documents == null ) {
            documents = new ArrayList<BSONObject>();
        }
        this.documents = documents;
    }


    public void addDocument( BSONObject document ) {
        documents.add( document );
    }


    public void addDocument( Map<?, ?> map ) {
        BSONObject b = new BasicBSONObject();
        b.putAll( map );
        documents.add( b );
    }


    @Override
    public void decode( ChannelBuffer buffer ) throws IOException {
        super.decode( buffer );

        flags = buffer.readInt();
        fullCollectionName = readCString( buffer );

        while ( buffer.readable() ) {
            documents.add( BSONUtils.decoder().readObject( new ChannelBufferInputStream( buffer ) ) );
        }
    }


    @Override
    public ChannelBuffer encode( ChannelBuffer buffer ) {
        int l = 20; // 5 ints * 4 bytes

        ByteBuffer fullCollectionNameBytes = getCString( fullCollectionName );
        l += fullCollectionNameBytes.capacity();

        List<ByteBuffer> encodedDocuments = encodeDocuments( documents );
        l += buffersSize( encodedDocuments );

        messageLength = l;

        buffer = super.encode( buffer );

        buffer.writeInt( flags );

        buffer.writeBytes( fullCollectionNameBytes );

        for ( ByteBuffer d : encodedDocuments ) {
            buffer.writeBytes( d );
        }

        return buffer;
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.mongo.protocol.OpCrud#doOp(org.apache.usergrid.mongo.MongoChannelHandler,
     * org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
     */
    @SuppressWarnings("unchecked")
    @Override
    public OpReply doOp( MongoChannelHandler handler, ChannelHandlerContext ctx, MessageEvent messageEvent ) {

        ApplicationInfo application = SubjectUtils.getApplication( Identifier.from( getDatabaseName() ) );

        if ( application == null ) {
            ctx.setAttachment( new IllegalArgumentException(
                    String.format( "Could not find application with name '%s' ", getDatabaseName() ) ) );
            return null;
        }


        EntityManager em = handler.getEmf().getEntityManager( application.getId() );


        for ( BSONObject document : documents ) {
            try {
                //special case to serialize mongo ObjectId if required
                Object id = document.get( "_id" );

                if ( id instanceof ObjectId ) {
                    document.put( "_id", ( ( ObjectId ) id ).toStringMongod() );
                }

                em.create( getCollectionName(), document.toMap() );
            }
            catch ( Exception e ) {
                logger.error( "Unable to insert mongo document {}", document, e );
                ctx.setAttachment( e );
            }
        }

        //insert never returns a response in mongo
        return null;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "OpInsert [flags=" + flags + ", documents=" + documents + ", fullCollectionName=" + fullCollectionName
                + ", messageLength=" + messageLength + ", requestID=" + requestID + ", responseTo=" + responseTo
                + ", opCode=" + opCode + "]";
    }
}
