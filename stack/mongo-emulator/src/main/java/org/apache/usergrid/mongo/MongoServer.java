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


import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.Properties;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.cassandra.EntityManagerFactoryImpl;
import org.apache.usergrid.services.ServiceManagerFactory;

import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SessionsSecurityManager;
import org.apache.shiro.realm.Realm;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class MongoServer {

    private static final Logger logger = LoggerFactory.getLogger( MongoServer.class );

    EntityManagerFactory emf;
    ServiceManagerFactory smf;
    ManagementService management;
    Realm realm;
    SessionsSecurityManager securityManager;
    Channel channel;
    Properties properties;


    public static void main( String[] args ) throws Exception {
        MongoServer server = new MongoServer();
        server.startSpring();
        server.startServer();
    }


    public MongoServer() {
    }


    @Autowired
    public void setEntityManagerFactory( EntityManagerFactory emf ) {
        this.emf = emf;
    }


    @Autowired
    public void setServiceManagerFactory( ServiceManagerFactory smf ) {
        this.smf = smf;
    }


    @Autowired
    public void setManagementService( ManagementService management ) {
        this.management = management;
    }


    @Autowired
    public void setRealm( Realm realm ) {
        this.realm = realm;
    }


    public Properties getProperties() {
        return properties;
    }


    @Autowired
    public void setProperties( Properties properties ) {
        this.properties = properties;
    }


    public String[] getApplicationContextLocations() {
        String[] locations = { "applicationContext.xml" };
        return locations;
    }


    public void startSpring() {

        String[] locations = getApplicationContextLocations();
        ApplicationContext ac = new ClassPathXmlApplicationContext( locations );

        AutowireCapableBeanFactory acbf = ac.getAutowireCapableBeanFactory();
        acbf.autowireBeanProperties( this, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false );
        acbf.initializeBean( this, "mongoServer" );

        assertNotNull( emf );
        assertTrue( "EntityManagerFactory is instance of EntityManagerFactoryImpl",
                emf instanceof EntityManagerFactoryImpl );
    }


    public void startServer() {

        if ( ( properties != null ) && ( Boolean
                .parseBoolean( properties.getProperty( "usergrid.mongo.disable", "false" ) ) ) ) {
            logger.info( "Usergrid Mongo Emulation Server Disabled" );
            return;
        }

        logger.info( "Starting Usergrid Mongo Emulation Server" );

        if ( realm != null ) {
            securityManager = new DefaultSecurityManager( realm );
        }

        // Configure the server.
        ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory( Executors.newCachedThreadPool(), Executors.newCachedThreadPool() ) );

        bootstrap.setOption( "child.bufferFactory", HeapChannelBufferFactory.getInstance( ByteOrder.LITTLE_ENDIAN ) );

        // Set up the pipeline factory.
        ExecutionHandler executionHandler =
                new ExecutionHandler( new OrderedMemoryAwareThreadPoolExecutor( 16, 1048576, 1048576 ) );
        // TODO if config'ed for SSL, start the SslMSPF instead, change port as well?
        bootstrap.setPipelineFactory(
                new MongoServerPipelineFactory( emf, smf, management, securityManager, executionHandler ) );

        // Bind and start to accept incoming connections.
        channel = bootstrap.bind( new InetSocketAddress( 27017 ) );

        logger.info( "Usergrid Mongo API Emulation Server accepting connections..." );
    }


    public void stopServer() {
        logger.info( "Stopping Usergrid Mongo Emulation Server" );
        if ( channel != null ) {
            channel.close();
            channel = null;
        }
        logger.info( "Usergrid Mongo API Emulation Server stopped..." );
    }
}
