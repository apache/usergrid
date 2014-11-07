/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */

package org.apache.usergrid.persistence.index.impl;


import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.safehaus.guicyfig.Env;
import org.safehaus.guicyfig.EnvironResource;
import org.safehaus.guicyfig.GuicyFigModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.persistence.core.util.AvailablePortFinder;
import org.apache.usergrid.persistence.index.IndexFig;

import com.amazonaws.util.StringUtils;
import com.google.inject.Guice;
import com.google.inject.Injector;


public class ElasticSearchResource extends EnvironResource {

    private static final Logger log = LoggerFactory.getLogger( ElasticSearchResource.class );

    private static Node node;

    private static int port;
    private static String host;
    
    private static boolean externalElasticSearch = false;



    public ElasticSearchResource() {
        super( Env.UNIT );
        try {
            Properties props = new Properties();
            props.load( ClassLoader.getSystemResourceAsStream( "project.properties" ) );
            host=(String)props.getProperty( "elasticsearch.host", "127.0.0.1" );
            port=Integer.valueOf(props.getProperty( "elasticsearch.port", "9300" )).intValue();
            String forkString = props.getProperty("elasticsearch.startup", "external");
            externalElasticSearch = "external".equals( forkString );

        } catch (Exception ex) {
            throw new RuntimeException("Error getting properties", ex);
        }
    }


    @Override
    protected void before() throws Throwable {
    	if(externalElasticSearch){
            System.setProperty( IndexFig.ELASTICSEARCH_HOSTS, host );
            System.setProperty( IndexFig.ELASTICSEARCH_PORT, port+"" );
    		
    	}else{
            startEs();
    	}
    }




    public static int getPort() {
        return port;
    }


    public synchronized ElasticSearchResource startEs(){
        if ( node != null ) {
            return this;
        }


        //override the system properties in the Archiaus env
        port = AvailablePortFinder.getNextAvailable( 9300 );

        final String host = "127.0.0.1";
        System.setProperty( IndexFig.ELASTICSEARCH_HOSTS, host );
        System.setProperty( IndexFig.ELASTICSEARCH_PORT, port + "" );

        //we have to create this AFTER we set our system properties, or they won't get picked upt
        Injector injector = Guice.createInjector( new GuicyFigModule( IndexFig.class ) );
        IndexFig indexFig = injector.getInstance( IndexFig.class );


        final String clusterName = indexFig.getClusterName();

        File tempDir;
        try {
            tempDir = getTempDirectory();
        }
        catch ( Exception ex ) {
            throw new RuntimeException( "Fatal error unable to create temp dir, start embedded ElasticSearch", ex );
        }


        Settings settings = ImmutableSettings.settingsBuilder()
                .put("cluster.name", clusterName)
                .put("network.publish_host", host)
                .put("transport.tcp.port", port)
                .put("discovery.zen.ping.multicast.enabled", "false")
                .put("node.http.enabled", false)
                .put("path.logs", tempDir.toString())
                .put("path.data", tempDir.toString())
                .put("index.store.type", "default")
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 1)
                .build();


        log.info( "-----------------------------------------------------------------------" );
        log.info( "Starting ElasticSearch embedded server settings: \n" + settings.getAsMap() );
        log.info( "-----------------------------------------------------------------------" );


        node = NodeBuilder.nodeBuilder().settings( settings ).clusterName( indexFig.getClusterName() ).client( false ).data( true ).build().start();

        Runtime.getRuntime().addShutdownHook( new Thread() {
            @Override
            public void run() {
                shutdown();
            }
        } );

        return this;
    }


    public static void shutdown() {
    	if(!externalElasticSearch){
            node.stop();
    	}
    }




    /**
     * Uses a project.properties file that Maven does substitution on to to replace the value of a property with the
     * path to the Maven build directory (a.k.a. target). It then uses this path to generate a random String which it
     * uses to append a path component to so a unique directory is selected. If already present it's deleted, then the
     * directory is created.
     *
     * @return a unique temporary directory
     *
     * @throws java.io.IOException if we cannot access the properties file
     */
    public static File getTempDirectory() throws IOException {
        File tmpdir;
        Properties props = new Properties();
        props.load( ClassLoader.getSystemResourceAsStream( "project.properties" ) );
        File basedir = new File( ( String ) props.get( "target.directory" ) );
        String comp = RandomStringUtils.randomAlphanumeric( 7 );
        tmpdir = new File( basedir, comp );

        if ( tmpdir.exists() ) {
            log.info( "Deleting directory: {}", tmpdir );
            FileUtils.forceDelete( tmpdir );
        }
        else {
            log.info( "Creating temporary directory: {}", tmpdir );
            FileUtils.forceMkdir( tmpdir );
        }

        return tmpdir;
    }
}
