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
package org.apache.usergrid.mongo;


import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.mongo.protocol.Message;
import org.apache.usergrid.mongo.protocol.OpDelete;
import org.apache.usergrid.mongo.protocol.OpGetMore;
import org.apache.usergrid.mongo.protocol.OpInsert;
import org.apache.usergrid.mongo.protocol.OpKillCursors;
import org.apache.usergrid.mongo.protocol.OpMsg;
import org.apache.usergrid.mongo.protocol.OpQuery;
import org.apache.usergrid.mongo.protocol.OpReply;
import org.apache.usergrid.mongo.protocol.OpUpdate;


public class MongoMessageDecoder extends FrameDecoder {

    private static final Logger logger = LoggerFactory.getLogger( MongoMessageDecoder.class );


    @Override
    protected Object decode( ChannelHandlerContext ctx, Channel channel, ChannelBuffer buf ) throws Exception {

        if ( buf.readableBytes() < 4 ) {
            logger.info( "Needed at least 4 bytes, only " + buf.readableBytes() + " available" );
            return null;
        }

        // logger.info("Mongo message decoding...");

        int length = buf.getInt( buf.readerIndex() );

        if ( length < 0 ) {
            logger.info( "Negative length " + length );
            return null;
        }

        if ( buf.readableBytes() < length ) {
            logger.info( "Needed " + length + " bytes, only " + buf.readableBytes() + " available" );
            return null;
        }

        // logger.info("Attempting to read " + length + " bytes");
        ChannelBuffer frame = buf.readSlice( length );

        int opCode = frame.getInt( frame.readerIndex() + 12 );

        // logger.info("Mongo message opcode " + opCode + " received");

        Message message = null;
        if ( opCode == Message.OP_DELETE ) {
            message = new OpDelete();
        }
        else if ( opCode == Message.OP_GET_MORE ) {
            message = new OpGetMore();
        }
        else if ( opCode == Message.OP_INSERT ) {
            message = new OpInsert();
        }
        else if ( opCode == Message.OP_KILL_CURSORS ) {
            message = new OpKillCursors();
        }
        else if ( opCode == Message.OP_MSG ) {
            message = new OpMsg();
        }
        else if ( opCode == Message.OP_QUERY ) {
            message = new OpQuery();
        }
        else if ( opCode == Message.OP_REPLY ) {
            message = new OpReply();
        }
        else if ( opCode == Message.OP_UPDATE ) {
            message = new OpUpdate();
        }

        if ( message != null ) {
            message.decode( frame );
        }
        else {
            logger.info( "Mongo unrecongnized message opcode " + opCode + " received" );
        }

        // logger.info(message);

        return message;
    }


    static MongoMessageDecoder _instance = new MongoMessageDecoder();


    public static Message decode( ChannelBuffer buf ) throws Exception {
        return ( Message ) _instance.decode( null, null, buf.duplicate() );
    }
}
