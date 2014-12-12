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
package org.apache.usergrid.cassandra;


import java.io.IOException;
import java.util.Properties;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.apache.commons.lang.ArrayUtils;


/**
 * A JUnit {@link org.junit.rules.ExternalResource} designed to set our system properties then start spring so it connects to cassandra correctly
 *
 */
public class CassandraResource extends ExternalResource {
    public static final Logger LOG = LoggerFactory.getLogger( CassandraResource.class );
    public static final int DEFAULT_RPC_PORT = 9160;
    public static final int DEFAULT_STORAGE_PORT = 7000;
    public static final int DEFAULT_SSL_STORAGE_PORT = 7001;
    public static final int DEFAULT_NATIVE_TRANSPORT_PORT = 9042;
    public static final String DEFAULT_HOST = "127.0.0.1";

    public static final String PROPERTIES_FILE = "project.properties";

    public static final String NATIVE_TRANSPORT_PORT_KEY = "native_transport_port";
    public static final String RPC_PORT_KEY = "rpc_port";
    public static final String STORAGE_PORT_KEY = "storage_port";
    public static final String SSL_STORAGE_PORT_KEY = "ssl_storage_port";

    private static final Object lock = new Object();

    private boolean initialized = false;
    private int rpcPort = DEFAULT_RPC_PORT;
    private int storagePort = DEFAULT_STORAGE_PORT;
    private int sslStoragePort = DEFAULT_SSL_STORAGE_PORT;
    private int nativeTransportPort = DEFAULT_NATIVE_TRANSPORT_PORT;

    private ConfigurableApplicationContext applicationContext;
    private SchemaManager schemaManager;

    private static CassandraResource instance;

    private static Properties properties = null;


    /**
     * Creates a Cassandra starting ExternalResource for JUnit test cases which uses the specified SchemaManager for
     * Cassandra.
     */
    CassandraResource( int rpcPort, int storagePort, int sslStoragePort, int nativeTransportPort ) {
        LOG.info( "Creating CassandraResource using {} for the ClassLoader.",
                Thread.currentThread().getContextClassLoader() );

        this.rpcPort = rpcPort;
        this.storagePort = storagePort;
        this.sslStoragePort = sslStoragePort;
        this.nativeTransportPort = nativeTransportPort;


        try {
            String[] locations = { "usergrid-properties-context.xml" };
            ConfigurableApplicationContext appContext = new ClassPathXmlApplicationContext( locations );

            Properties properties = ( Properties ) appContext.getBean( "properties" );
            properties.putAll( ArrayUtils.toMap( this.getProjectProperties().entrySet().toArray( new Object[] { } ) ) );
        }
        catch ( Exception ex ) {
            throw new RuntimeException( "Error getting properties", ex );
        }
    }


    /**
     * Gets the rpcPort for Cassandra.
     *
     * @return the rpc_port (in yaml file) used by Cassandra
     */
    public int getRpcPort() {
        return rpcPort;
    }




    /**
     * Gets a bean from the application context.
     *
     * @param requiredType the type of the bean
     * @param <T> the type of the bean
     *
     * @return the bean
     */
    public <T> T getBean( String name, Class<T> requiredType ) {
        protect();
        return applicationContext.getBean( name, requiredType );
    }


    /**
     * Gets a bean from the application context.
     *
     * @param requiredType the type of the bean
     * @param <T> the type of the bean
     *
     * @return the bean
     */
    public <T> T getBean( Class<T> requiredType ) {
        protect();
        return applicationContext.getBean( requiredType );
    }


    /**
     * Gets whether this resource is ready to use.
     *
     * @return true if ready to use, false otherwise
     */
    public boolean isReady() {
        return initialized;
    }




    /**
     * Protects against IDE or command line runs of a unit test which when starting the test outside of the Suite will
     * not start the resource. This makes sure the resource is automatically started on a usage attempt.
     */
    private void protect() {
        if ( !isReady() ) {
            try {
                before();
            }
            catch ( Throwable t ) {
                LOG.error( "Failed to start up Cassandra.", t );
                throw new RuntimeException( t );
            }
        }
    }


    /**
     * Starts up Cassandra before TestSuite or test Class execution.
     *
     * @throws Throwable if we cannot start up Cassandra
     */
    @Override
    protected void before() throws Throwable {
        /*
         * Note that the lock is static so it is JVM wide which prevents other
         * instances of this class from making changes to the Cassandra system
         * properties while we are initializing Cassandra with unique settings.
         */

        synchronized ( lock ) {
            super.before();

            if ( isReady() ) {
                return;
            }

            startSpring();
        }
    }


    private void startSpring() throws Throwable {
        LOG.info( "-------------------------------------------------------------------" );
        LOG.info( "Initializing External Cassandra" );
        LOG.info( "-------------------------------------------------------------------" );
        LOG.info( "before() test, setting system properties for ports : "
                        + "[rpc, storage, sslStorage, native] = [{}, {}, {}, {}]",
                new Object[] { rpcPort, storagePort, sslStoragePort, nativeTransportPort } );
        String[] locations = { "usergrid-test-context.xml" };
        applicationContext = new ClassPathXmlApplicationContext( locations );
        applicationContext.refresh();
        initialized = true;
        lock.notifyAll();
    }


    /**
     * Loads the specified {@link SchemaManager} or the default manager if the manager name that is provided is null.
     *
     *
     */
    private void loadSchemaManager() {
        if ( !applicationContext.isActive() ) {
            LOG.info( "Restarting context..." );
            applicationContext.refresh();
        }


        LOG.info( "The SchemaManager is not specified - using the default SchemaManager impl" );
        this.schemaManager = applicationContext.getBean( SchemaManager.class );

        schemaManager.create();
        schemaManager.populateBaseData();
    }


    public static CassandraResource setAllocatedPorts() {
        synchronized ( lock ) {

            //don't keep re-initializing if it's already done
            if(instance != null){
                return instance;
            }

            Properties props = new Properties();
            try {
                props.load( ClassLoader.getSystemResourceAsStream( "project.properties" ) );
            }
            catch ( IOException e ) {
                LOG.error( "Unable to load project properties: {}", e.getLocalizedMessage() );
            }
            int rpcPort = Integer.parseInt(
                    props.getProperty( "cassandra.rpcPort", Integer.toString( CassandraResource.DEFAULT_RPC_PORT ) ) );
            int storagePort = Integer.parseInt( props.getProperty( "cassandra.storagePort",
                    Integer.toString( CassandraResource.DEFAULT_STORAGE_PORT ) ) );
            int sslStoragePort = Integer.parseInt( props.getProperty( "cassandra.sslPort",
                    Integer.toString( CassandraResource.DEFAULT_SSL_STORAGE_PORT ) ) );
            int nativeTransportPort = Integer.parseInt( props.getProperty( "cassandra.nativeTransportPort",
                    Integer.toString( CassandraResource.DEFAULT_NATIVE_TRANSPORT_PORT ) ) );
            String host = props.getProperty( "cassandra.host", DEFAULT_HOST );

            System.setProperty( "cassandra.url", host + ":" + Integer.toString( rpcPort ) );
            System.setProperty( "cassandra.cluster", props.getProperty( "cassandra.cluster", "Usergrid" ) );
            System.setProperty( "cassandra-foreground", "true" );
            System.setProperty( "log4j.defaultInitOverride", "true" );
            System.setProperty( "log4j.configuration", "log4j.properties" );
            System.setProperty( "cassandra.ring_delay_ms", "100" );

            System.setProperty( "cassandra." + RPC_PORT_KEY, Integer.toString( rpcPort ) );
            System.setProperty( "cassandra." + STORAGE_PORT_KEY, Integer.toString( storagePort ) );
            System.setProperty( "cassandra." + SSL_STORAGE_PORT_KEY, Integer.toString( sslStoragePort ) );
            System.setProperty( "cassandra." + NATIVE_TRANSPORT_PORT_KEY, Integer.toString( nativeTransportPort ) );

            LOG.info( "project.properties loaded properties for ports : "
                            + "[rpc, storage, sslStorage, native] = [{}, {}, {}, {}]",
                    new Object[] { rpcPort, storagePort, sslStoragePort, nativeTransportPort } );


            instance = new CassandraResource( rpcPort, storagePort, sslStoragePort, nativeTransportPort );

            LOG.info( "Created a new instance of CassandraResource: {}", instance );
            LOG.info( "Cassandra using ports {} and {}", storagePort, sslStoragePort );



            return instance;
        }
    }


    /**
     * Creates a new instance of the CassandraResource with rpc and storage ports that may or may not be the default
     * ports. If either port is taken, an alternative free port is found.
     *
     * @return a new CassandraResource with possibly non-default ports
     */
    public static CassandraResource setPortsAndStartSpring() {
        return setAllocatedPorts();
    }



    public static Properties getProjectProperties() throws IOException {
        if ( properties == null ) {
            properties = new Properties();
            properties.load( ClassLoader.getSystemResourceAsStream( PROPERTIES_FILE ) );
        }
        return properties;
    }


    @Override
    public String toString() {
        return "CassandraResource{" +
                "initialized=" + initialized +
                ", rpcPort=" + rpcPort +
                ", storagePort=" + storagePort +
                ", sslStoragePort=" + sslStoragePort +
                ", nativeTransportPort=" + nativeTransportPort +
                ", applicationContext=" + applicationContext +
                ", schemaManager=" + schemaManager +
                "} " + super.toString();
    }
}
