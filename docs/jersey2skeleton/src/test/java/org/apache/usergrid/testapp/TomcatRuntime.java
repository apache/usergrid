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
package org.apache.usergrid.testapp;


import com.google.common.io.Files;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.io.File;


/**
 * Start and stop embedded Tomcat.
 */
public class TomcatRuntime extends ExternalResource {
    private static final Logger log = LoggerFactory.getLogger( TomcatRuntime.class );


    private static final String WEBAPP_PATH = System.getProperty("webapp.directory");

    private static TomcatRuntime instance;

    public final TomcatInstance tomcat;


    private TomcatRuntime() {

        tomcat = new TomcatInstance( WEBAPP_PATH );
        tomcat.startTomcat();

        //stop on JVM shutdown
        Runtime.getRuntime().addShutdownHook( new Thread() {
            @Override
            public void run() {
                tomcat.stopTomcat();
            }
        } );
    }


    /**
     * Get the instance of the tomcat runtime and starts the tomcat singleton.  Starts tomcat once per JVM
     * @return
     */
    public static synchronized TomcatRuntime getInstance() {
        if ( instance == null ) {


            instance = new TomcatRuntime();
        }

        return instance;
    }


    /**
     * Get the port tomcat is running on
     */
    public int getPort() {
        return tomcat.getPort();
    }


    /**
     * Inner class of tomcat runtime
     */
    private static class TomcatInstance {

        public static final int THREADS_PERPROC = 25;

        private final String webAppsPath;

        private Tomcat tomcat = null;
        private int port;

        private boolean started = false;


        private TomcatInstance( final String webAppsPath ) {this.webAppsPath = webAppsPath;}


        /**
         * Start the tomcat instance
         */
        public void startTomcat() {
            try {

                //we don't want to use all our threads, we'll kill the box
                final int availableProcessors = Runtime.getRuntime().availableProcessors();
                final int usedProcs = Math.min( 2, availableProcessors );
                final int threads = usedProcs * THREADS_PERPROC;


                File dataDir = Files.createTempDir();
                dataDir.deleteOnExit();

                port = AvailablePortFinder.getNextAvailable( 9998 + RandomUtils.nextInt( 10 ) );

                tomcat = new Tomcat();
                tomcat.setBaseDir( dataDir.getAbsolutePath() );
                tomcat.setPort( port );


                tomcat.getConnector().setAttribute( "maxThreads", "" + threads );

                tomcat.addWebapp( "/", new File( webAppsPath ).getAbsolutePath() );


                log.info( "-----------------------------------------------------------------" );
                log.info( "Starting Tomcat embedded port {} dir {}", port, dataDir.getAbsolutePath() );
                log.info( "-----------------------------------------------------------------" );
                tomcat.start();

                waitForTomcat();

            }
            catch ( Exception e ) {
                throw new RuntimeException( "Couldn't start tomcat", e );
            }
        }


        /**
         * Stop the embedded tomcat process
         */
        public void stopTomcat() {
            try {
                tomcat.stop();
            }
            catch ( LifecycleException e ) {
                throw new RuntimeException( "Unable to stop tomcat", e );
            }
        }


        public int getPort() {
            return port;
        }


        private void waitForTomcat() throws RuntimeException {
            String url = "http://localhost:" + port + "/status";
            int count = 0;
            while ( count++ < 30 ) {
                try {
                    Thread.sleep( 1000 );
                    Client c = ClientBuilder.newClient();
                    WebTarget wr = c.target( url );
                    wr.request().get( String.class );
                    log.info( "Tomcat is started." );
                    started = true;
                    break;
                }
                catch ( Exception e ) {
                    log.info( "Waiting for Tomcat on url {}", url );
                }
            }
            if ( !started ) {
                throw new RuntimeException( "Tomcat process never started." );
            }
        }
    }
}
