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
package org.apache.usergrid.mongo.testproxy;


import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.mongo.protocol.Message;
import org.apache.usergrid.mongo.protocol.OpReply;


public class MongoProxyServerHandler extends SimpleChannelUpstreamHandler {

    private static final Logger logger = LoggerFactory.getLogger( MongoProxyServerHandler.class );


    @Override
    public void messageReceived( ChannelHandlerContext ctx, MessageEvent e ) {

        Message message = null;
        if ( e.getMessage() instanceof Message ) {
            message = ( Message ) e.getMessage();
        }

        OpReply reply = OpReply.errorReply( "not implemented" );

        if ( message != null ) {
            logger.info( message.getClass().getName() );
            reply.setResponseTo( message.getRequestID() );
        }

        e.getChannel().write( reply );
    }


    @Override
    public void exceptionCaught( ChannelHandlerContext ctx, ExceptionEvent e ) {
        logger.warn( "Unexpected exception from downstream.", e.getCause() );
        e.getChannel().close();
    }
}
