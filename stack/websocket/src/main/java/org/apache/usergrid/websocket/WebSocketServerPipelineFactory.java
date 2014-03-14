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
package org.apache.usergrid.websocket;


import javax.net.ssl.SSLEngine;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.ssl.SslHandler;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.services.ServiceManagerFactory;

import org.apache.shiro.mgt.SessionsSecurityManager;

import static org.jboss.netty.channel.Channels.pipeline;


public class WebSocketServerPipelineFactory implements ChannelPipelineFactory {

    private final ExecutionHandler executionHandler;
    private final EntityManagerFactory emf;
    private final ServiceManagerFactory smf;
    private final ManagementService management;
    private final SessionsSecurityManager securityManager;
    private final boolean ssl;


    public WebSocketServerPipelineFactory( EntityManagerFactory emf, ServiceManagerFactory smf,
                                           ManagementService management, SessionsSecurityManager securityManager,
                                           ExecutionHandler executionHandler, boolean ssl ) {
        this.emf = emf;
        this.smf = smf;
        this.management = management;
        this.securityManager = securityManager;
        this.executionHandler = executionHandler;
        this.ssl = ssl;
    }


    @Override
    public ChannelPipeline getPipeline() throws Exception {
        // Create a default pipeline implementation.
        ChannelPipeline pipeline = pipeline();
        if ( ssl ) {
            SSLEngine sslEngine = WebSocketSslContextFactory.getServerContext().createSSLEngine();
            sslEngine.setUseClientMode( false );
            pipeline.addLast( "ssl", new SslHandler( sslEngine ) );
        }
        pipeline.addLast( "decoder", new HttpRequestDecoder() );
        pipeline.addLast( "aggregator", new HttpChunkAggregator( 65536 ) );
        pipeline.addLast( "encoder", new HttpResponseEncoder() );
        pipeline.addLast( "execution", executionHandler );
        pipeline.addLast( "handler", new WebSocketChannelHandler( emf, smf, management, securityManager, ssl ) );
        return pipeline;
    }
}
