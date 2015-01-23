/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.cassandra;


import java.io.IOException;
import java.util.Properties;

import org.safehaus.guicyfig.Env;
import org.safehaus.guicyfig.EnvironResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Create the resource that sets the cassandra properties
 *
 * TODO move from instance to static
 */
public class CassandraResource extends EnvironResource {


    public static final Logger LOG = LoggerFactory.getLogger( SpringResource.class );
    public static final String DEFAULT_HOST = "127.0.0.1";


    public static final int DEFAULT_RPC_PORT = 9160;


    public static final String RPC_PORT_KEY = "rpc_port";

    private static final Object lock = new Object();


    private static int port;
    private static String host;
    private static String hostUrl;


    private static boolean initialized = false;


    public CassandraResource() {
        super( Env.UNIT );
    }


    /**
     * Start the cassandra setup
     * @throws Throwable
     */
    public void start()  {
        before();
    }


    @Override
    protected void before()  {
        if ( initialized ) {
            return;
        }

        synchronized ( lock ) {

            if ( initialized ) {
                return;
            }

            Properties props = new Properties();
            try {
                props.load( ClassLoader.getSystemResourceAsStream( "project.properties" ) );
            }
            catch ( IOException e ) {
                LOG.error( "Unable to load project properties: {}", e.getLocalizedMessage() );
            }
            port = Integer.parseInt(
                    props.getProperty( "cassandra.rpcPort", Integer.toString( DEFAULT_RPC_PORT ) ) );

            host = props.getProperty( "cassandra.host", DEFAULT_HOST );


            hostUrl = host + ":" + Integer.toString( port );


            System.setProperty( "cassandra.url", hostUrl );
            System.setProperty( "cassandra.cluster", props.getProperty( "cassandra.cluster", "Usergrid" ) );
            System.setProperty( "cassandra-foreground", "true" );
            System.setProperty( "log4j.defaultInitOverride", "true" );
            System.setProperty( "log4j.configuration", "log4j.properties" );
            System.setProperty( "cassandra.ring_delay_ms", "100" );

            System.setProperty( "cassandra." + RPC_PORT_KEY, Integer.toString( port ) );

            LOG.info( "project.properties loaded properties for ports : " + "[rpc] = [{}]", new Object[] { port } );


            initialized = true;
        }
    }


    /**
     * Get the cassandra host
     * @return
     */
    public static int getPort() {
        return port;
    }


    /**
     * Get the cassandra host
     * @return
     */
    public static String getHost() {return host;}


}
