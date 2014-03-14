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


import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MongoProxyServer {

    private static final Logger logger = LoggerFactory.getLogger( MongoProxyServer.class );


    public static void main( String[] args ) throws Exception {
        logger.info( "Starting Usergrid Mongo Proxy Server" );

        // Configure the server.
        Executor executor = Executors.newCachedThreadPool();
        ServerBootstrap bootstrap = new ServerBootstrap( new NioServerSocketChannelFactory( executor, executor ) );

        bootstrap.setOption( "child.bufferFactory", HeapChannelBufferFactory.getInstance( ByteOrder.LITTLE_ENDIAN ) );

        ClientSocketChannelFactory cf = new NioClientSocketChannelFactory( executor, executor );

        bootstrap.setPipelineFactory( new MongoProxyPipelineFactory( cf, "localhost", 12345 ) );

        bootstrap.bind( new InetSocketAddress( 27017 ) );

        logger.info( "Usergrid Mongo Proxy Server accepting connections..." );
    }
}
