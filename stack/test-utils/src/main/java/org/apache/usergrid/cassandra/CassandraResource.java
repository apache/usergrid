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


import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertySource;
import org.yaml.snakeyaml.Yaml;
import org.apache.cassandra.service.CassandraDaemon;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;

/**
 * A JUnit {@link org.junit.rules.ExternalResource} designed to start up Cassandra once in a TestSuite or test Class as
 * a shared external resource across test cases and shut it down after the TestSuite has completed.
 * <p/>
 * This external resource is completely isolated in terms of the files used and the ports selected if the {@link
 * AvailablePortFinder} is used with it.
 * <p/>
 * Note that for this resource to work properly, a project.properties file must be placed in the src/test/resources
 * directory with the following stanza in the project pom's build section:
 * <p/>
 * <testResources> <testResource> <directory>src/test/resources</directory> <filtering>true</filtering> <includes>
 * <include>**\/*.properties</include> <include>**\/*.xml</include> </includes> </testResource> </testResources>
 * <p/>
 * The following property expansion macro should be placed in this project.properties file:
 * <p/>
 * target.directory=${pom.build.directory}
 */
public class CassandraResource extends ExternalResource {
    public static final Logger LOG = LoggerFactory.getLogger( CassandraResource.class );
    public static final int DEFAULT_RPC_PORT = 9160;
    public static final int DEFAULT_STORAGE_PORT = 7000;
    public static final int DEFAULT_SSL_STORAGE_PORT = 7001;
    public static final int DEFAULT_NATIVE_TRANSPORT_PORT = 9042;
    public static final String DEFAULT_HOST = "127.0.0.1";

    public static final String PROPERTIES_FILE = "project.properties";
    public static final String TARGET_DIRECTORY_KEY = "target.directory";
    public static final String DATA_FILE_DIR_KEY = "data_file_directories";
    public static final String COMMIT_FILE_DIR_KEY = "commitlog_directory";
    public static final String SAVED_CACHES_DIR_KEY = "saved_caches_directory";

    public static final String NATIVE_TRANSPORT_PORT_KEY = "native_transport_port";
    public static final String RPC_PORT_KEY = "rpc_port";
    public static final String STORAGE_PORT_KEY = "storage_port";
    public static final String SSL_STORAGE_PORT_KEY = "ssl_storage_port";
    public static final String JAMM_PATH = "jamm.path";

    private static final Object lock = new Object();

    private final File tempDir;
    private final String schemaManagerName;

    private boolean initialized = false;
    private int rpcPort = DEFAULT_RPC_PORT;
    private int storagePort = DEFAULT_STORAGE_PORT;
    private int sslStoragePort = DEFAULT_SSL_STORAGE_PORT;
    private int nativeTransportPort = DEFAULT_NATIVE_TRANSPORT_PORT;

    private ConfigurableApplicationContext applicationContext;
    private CassandraDaemon cassandraDaemon;
    private SchemaManager schemaManager;

    private static CassandraResource instance;
    private Thread shutdown;

    private Process process = null;

    private static Properties properties = null;

    private boolean forkCassandra = false;
    private boolean externalCassandra = false;

     /**
     * Creates a Cassandra starting ExternalResource for JUnit test cases which uses the 
     * default SchemaManager for Cassandra.
     */
    @SuppressWarnings("UnusedDeclaration")
    CassandraResource() throws IOException {
        this( null, DEFAULT_RPC_PORT, DEFAULT_STORAGE_PORT, DEFAULT_SSL_STORAGE_PORT, 
                DEFAULT_NATIVE_TRANSPORT_PORT );

    }


    /**
     * Creates a Cassandra starting ExternalResource for JUnit test cases which uses the 
     * specified SchemaManager for Cassandra.
     */
    CassandraResource( String schemaManagerName, int rpcPort, int storagePort, int sslStoragePort,
                       int nativeTransportPort ) {
        LOG.info( "Creating CassandraResource using {} for the ClassLoader.",
                Thread.currentThread().getContextClassLoader() );

        this.schemaManagerName = schemaManagerName;
        this.rpcPort = rpcPort;
        this.storagePort = storagePort;
        this.sslStoragePort = sslStoragePort;
        this.nativeTransportPort = nativeTransportPort;

        try {
            this.tempDir = getTempDirectory();
        }
        catch ( IOException e ) {
            LOG.error( "Failed to create temporary directory for Cassandra instance.", e );
            throw new RuntimeException( e );
        }

        try {
            String[] locations = { "usergrid-properties-context.xml" };
            ConfigurableApplicationContext appContext = 
                    new ClassPathXmlApplicationContext( locations );
            
            Properties properties = (Properties)appContext.getBean("properties");
            properties.putAll(ArrayUtils.toMap(this.getProjectProperties().entrySet().toArray(new Object[]{})));
            String forkString = properties.getProperty("cassandra.startup");
            forkCassandra = "forked".equals( forkString );
            externalCassandra = "external".equals( forkString );

        } catch (Exception ex) {
            throw new RuntimeException("Error getting properties", ex);
        }
//        throw new RuntimeException("My debugging skills are terrible!");
    }


    /**
     * Creates a Cassandra starting ExternalResource for JUnit test cases which uses the specified 
     * SchemaManager for Cassandra.
     */
    public CassandraResource( int rpcPort, int storagePort, int sslStoragePort, int nativeTransportPort ) throws IOException {
        this( null, rpcPort, storagePort, sslStoragePort, nativeTransportPort );
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
     * Gets the storagePort for Cassandra.
     *
     * @return the storage_port (in yaml file) used by Cassandra
     */
    public int getStoragePort() {
        return storagePort;
    }


    /**
     * Gets the sslStoragePort for Cassandra.
     *
     * @return the sslStoragePort
     */
    public int getSslStoragePort() {
        return sslStoragePort;
    }


    public int getNativeTransportPort() {
        return nativeTransportPort;
    }


    /**
     * Gets the temporary directory created for this CassandraResource.
     *
     * @return the temporary directory
     */
    @SuppressWarnings("UnusedDeclaration")
    public File getTemporaryDirectory() {
        return tempDir;
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


    @Override
    public String toString() {
        return "\n" + "cassandra.yaml = " + new File(tempDir, "cassandra.yaml") + "\n" + RPC_PORT_KEY + " = " + rpcPort + "\n" + STORAGE_PORT_KEY + " = " + storagePort + "\n" + SSL_STORAGE_PORT_KEY + " = " + sslStoragePort + "\n" + NATIVE_TRANSPORT_PORT_KEY + " = " + nativeTransportPort + "\n" + DATA_FILE_DIR_KEY + " = " + new File(tempDir, "data").toString() + "\n" + COMMIT_FILE_DIR_KEY + " = " + new File(tempDir, "commitlog").toString() + "\n" + SAVED_CACHES_DIR_KEY + " = " + new File(tempDir, "saved_caches").toString() + "\n";
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
            
            if ( forkCassandra ) {
                startCassandraForked();
            } else if (externalCassandra) {
              startCassandraExternal();
            }else {
              
                startCassandraEmbedded();
            }
        }
    }
    private void addShutdownHook(){
        Runtime.getRuntime().addShutdownHook( new Thread() {
            @Override
            public void run() {
                after();
            }
        } );

    }
    private void startCassandraEmbedded() throws Throwable {

        LOG.info( "-------------------------------------------------------------------");
        LOG.info( "Initializing Embedded Cassandra at {} ...", tempDir.toString() );
        LOG.info( "-------------------------------------------------------------------");

        // Create temp directory, setup to create new File configuration there
        File newYamlFile = new File( tempDir, "cassandra.yaml" );
        URL newYamlUrl = FileUtils.toURLs( new File[] { newYamlFile } )[0];

        // Read the original yaml file, make changes, and dump to new position in tmpdir
        Yaml yaml = new Yaml();
        @SuppressWarnings("unchecked") Map<String, Object> map =
                ( Map<String, Object> ) yaml.load( ClassLoader.getSystemResourceAsStream( "cassandra.yaml" ) );
        map.put( RPC_PORT_KEY, getRpcPort() );
        map.put( STORAGE_PORT_KEY, getStoragePort() );
        map.put( SSL_STORAGE_PORT_KEY, getSslStoragePort() );
        map.put( NATIVE_TRANSPORT_PORT_KEY, getNativeTransportPort() );
        map.put( COMMIT_FILE_DIR_KEY, new File( tempDir, "commitlog" ).toString() );
        map.put( DATA_FILE_DIR_KEY, new String[] { new File( tempDir, "data" ).toString() } );
        map.put( SAVED_CACHES_DIR_KEY, new File( tempDir, "saved_caches" ).toString() );
        FileWriter writer = new FileWriter( newYamlFile );
        yaml.dump( map, writer );
        writer.flush();
        writer.close();

        // Fire up Cassandra by setting configuration to point to new yaml file
        System.setProperty( "cassandra.url", "localhost:" + Integer.toString( rpcPort ) );
        System.setProperty( "cassandra-foreground", "true" );
        System.setProperty( "log4j.defaultInitOverride", "true" );
        System.setProperty( "log4j.configuration", "log4j.properties" );
        System.setProperty( "cassandra.ring_delay_ms", "100" );
        System.setProperty( "cassandra.config", newYamlUrl.toString() );
        System.setProperty( "cassandra.tempName", tempDir.getName() );

        
        //while ( !AvailablePortFinder.available( rpcPort ) || rpcPort == 9042 ) {
        // why previously has a or condition of rpc == 9042?
        while ( !AvailablePortFinder.available( rpcPort ) ) {
            rpcPort++;
        }
        
        while ( !AvailablePortFinder.available( storagePort ) ) {
            storagePort++;
        }
        
        while ( !AvailablePortFinder.available( sslStoragePort ) ) {
            sslStoragePort++;
        }
        
        while ( !AvailablePortFinder.available( nativeTransportPort ) ) {
            nativeTransportPort++;
        }

        System.setProperty( "cassandra." + RPC_PORT_KEY, Integer.toString( rpcPort ) );
        System.setProperty( "cassandra." + STORAGE_PORT_KEY, Integer.toString( storagePort ) );
        System.setProperty( "cassandra." + SSL_STORAGE_PORT_KEY, Integer.toString( sslStoragePort ) );
        System.setProperty( "cassandra." + NATIVE_TRANSPORT_PORT_KEY, Integer.toString( nativeTransportPort ) );

        LOG.info("before() test, setting system properties for ports : "
                + "[rpc, storage, sslStorage, native] = [{}, {}, {}, {}]", 
                new Object[] {rpcPort, storagePort, sslStoragePort, nativeTransportPort});
        if ( !newYamlFile.exists() ) {
            throw new RuntimeException( "Cannot find new Yaml file: " + newYamlFile );
        }
        
        cassandraDaemon = new CassandraDaemon();
        cassandraDaemon.activate();

//        Runtime.getRuntime().addShutdownHook( new Thread() {
//            @Override
//            public void run() {
//                after();
//            }
//        } );
        addShutdownHook();
        String[] locations = { "usergrid-test-context.xml" };
        applicationContext = new ClassPathXmlApplicationContext( locations );

        loadSchemaManager( schemaManagerName );
        initialized = true;
        LOG.info( "External Cassandra resource at {} is ready!", tempDir.toString() );
        lock.notifyAll();
    }


    private void startCassandraForked() throws Throwable {

        LOG.info( "-------------------------------------------------------------------");
        LOG.info( "Initializing Forked Cassandra at {} ...", tempDir.toString() );
        LOG.info( "-------------------------------------------------------------------");

        // Create temp directory, setup to create new File configuration there
        File newYamlFile = new File( tempDir, "cassandra.yaml" );
        URL newYamlUrl = FileUtils.toURLs( new File[] { newYamlFile } )[0];

        // Read the original yaml file, make changes, and dump to new position in tmpdir
        Yaml yaml = new Yaml();
        @SuppressWarnings("unchecked") Map<String, Object> map =
                ( Map<String, Object> ) yaml.load( ClassLoader.getSystemResourceAsStream( "cassandra.yaml" ) );
        map.put( RPC_PORT_KEY, getRpcPort() );
        map.put( STORAGE_PORT_KEY, getStoragePort() );
        map.put( SSL_STORAGE_PORT_KEY, getSslStoragePort() );
        map.put( NATIVE_TRANSPORT_PORT_KEY, getNativeTransportPort() );
        map.put( COMMIT_FILE_DIR_KEY, new File( tempDir, "commitlog" ).toString() );
        map.put( DATA_FILE_DIR_KEY, new String[] { new File( tempDir, "data" ).toString() } );
        map.put( SAVED_CACHES_DIR_KEY, new File( tempDir, "saved_caches" ).toString() );
        FileWriter writer = new FileWriter( newYamlFile );
        yaml.dump( map, writer );
        writer.flush();
        writer.close();

        // Fire up Cassandra by setting configuration to point to new yaml file
        System.setProperty( "cassandra.url", "localhost:" + Integer.toString( rpcPort ) );
        System.setProperty( "cassandra-foreground", "true" );
        System.setProperty( "log4j.defaultInitOverride", "true" );
        System.setProperty( "log4j.configuration", "log4j.properties" );
        System.setProperty( "cassandra.ring_delay_ms", "100" );
        System.setProperty( "cassandra.config", newYamlUrl.toString() );
        System.setProperty( "cassandra.tempName", tempDir.getName() );

        
        //while ( !AvailablePortFinder.available( rpcPort ) || rpcPort == 9042 ) {
        // why previously has a or condition of rpc == 9042?
        while ( !AvailablePortFinder.available( rpcPort ) ) {
            rpcPort++;
        }
        
        while ( !AvailablePortFinder.available( storagePort ) ) {
            storagePort++;
        }
        
        while ( !AvailablePortFinder.available( sslStoragePort ) ) {
            sslStoragePort++;
        }
        
        while ( !AvailablePortFinder.available( nativeTransportPort ) ) {
            nativeTransportPort++;
        }

        System.setProperty( "cassandra." + RPC_PORT_KEY, Integer.toString( rpcPort ) );
        System.setProperty( "cassandra." + STORAGE_PORT_KEY, Integer.toString( storagePort ) );
        System.setProperty( "cassandra." + SSL_STORAGE_PORT_KEY, Integer.toString( sslStoragePort ) );
        System.setProperty( "cassandra." + NATIVE_TRANSPORT_PORT_KEY, Integer.toString( nativeTransportPort ) );

        LOG.info("before() test, setting system properties for ports : "
                + "[rpc, storage, sslStorage, native] = [{}, {}, {}, {}]", 
                new Object[] {rpcPort, storagePort, sslStoragePort, nativeTransportPort});

        if ( !newYamlFile.exists() ) {
            throw new RuntimeException( "Cannot find new Yaml file: " + newYamlFile );
        }
        
        String javaHome = (String)System.getenv("JAVA_HOME");

        String maxMemory = "-Xmx1000m";

        ProcessBuilder pb = new ProcessBuilder(javaHome + "/bin/java", 
                getJammArgument(), maxMemory,
                "org.apache.usergrid.cassandra.CassandraMain", 
                newYamlUrl.toString(), tempDir.getName(), 
                getTargetDir() + "/src/test/resources/log4j.properties",
                ""+rpcPort, ""+storagePort, ""+sslStoragePort, ""+nativeTransportPort );

        // ensure Cassandra gets same classpath we have, but with...
        String classpath = System.getProperty("java.class.path");
        List<String> path = new ArrayList<String>();

        String parts[] = classpath.split( File.pathSeparator );
        for ( String part : parts ) {
            if ( part.endsWith("test-classes") ) {
                continue;
            }
            path.add(part);
        }
        String newClasspath = StringUtils.join( path, File.pathSeparator );

        Map<String, String> env = pb.environment();
        StringBuilder sb = new StringBuilder();
        sb.append( newClasspath );
        env.put("CLASSPATH", sb.toString());

        pb.redirectErrorStream(true);

        process = pb.start();

        // use thread to log Cassandra output
        new Thread( new Runnable() {
            @Override
            public void run() {
                BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = null;
                try {
                    while ((line = br.readLine()) != null) {
                        LOG.info(line);
                    }

                } catch (Exception ex) {
                    LOG.error("Error reading from Cassandra process", ex);
                    return;
                } 
            }
        }).start();

//        Runtime.getRuntime().addShutdownHook( new Thread() {
//            @Override
//            public void run() {
//                after();
//            }
//        } );
        addShutdownHook();
        // give C* time to start
        Thread.sleep(5000);

        String[] locations = { "usergrid-test-context.xml" };
        applicationContext = new ClassPathXmlApplicationContext( locations );

        loadSchemaManager( schemaManagerName );
        initialized = true;
        LOG.info( "External Cassandra resource at {} is ready!", tempDir.toString() );
        lock.notifyAll();
    }
    private void startCassandraExternal() throws Throwable {
        LOG.info( "-------------------------------------------------------------------");
        LOG.info( "Initializing External Cassandra");
        LOG.info( "-------------------------------------------------------------------");
        LOG.info("before() test, setting system properties for ports : "
                + "[rpc, storage, sslStorage, native] = [{}, {}, {}, {}]", 
                new Object[] {rpcPort, storagePort, sslStoragePort, nativeTransportPort});
        Thread.sleep(5000);
        String[] locations = { "usergrid-test-context.xml" };
        applicationContext = new ClassPathXmlApplicationContext( locations );
//        PropertySource ps=new PropertySource<String>();
//        applicationContext.getEnvironment().getPropertySources().addLast(ps);
        applicationContext.refresh();
        loadSchemaManager( schemaManagerName );
        initialized = true;
        
        LOG.info( "External Cassandra resource at {} is ready!", tempDir.toString() );
        lock.notifyAll();
      
    }

    /** Stops Cassandra after a TestSuite or test Class executes. */
    @Override
    protected synchronized void after() {
        super.after();
        if ( process != null ) {
            process.destroy();
        }
        else { 
            shutdown = new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.currentThread();
            Thread.sleep( 100L );
                    }
                    catch ( InterruptedException ignored ) {}
                    if(externalCassandra){
                        LOG.info( "Cleaning up external Cassandra instance");
                    }else{
                        LOG.info( "Shutting down Cassandra instance in {}", tempDir.toString() );
                    }

                    if ( schemaManager != null ) {
                        LOG.info( "Destroying schemaManager..." );
                        try {
                            schemaManager.destroy();
                        }
                        catch ( Exception e ) {
                            LOG.error( "Ignoring failures while dropping keyspaces: {}", e.getMessage() );
                        }

                        LOG.info( "SchemaManager destroyed..." );
                    }

                    applicationContext.stop();
                    LOG.info( "ApplicationContext stopped..." );

                    try {
                        if ( !externalCassandra && cassandraDaemon != null ) {
                            LOG.info( "Deactivating CassandraDaemon..." );
                            cassandraDaemon.deactivate();
                        }
                    }
                    catch ( Exception ex ) {
                        LOG.error("Error deactivating Cassandra", ex);
                    }
                }
            };

            shutdown.start();
        }
    }


    /**
     * Loads the specified {@link SchemaManager} or the default manager if the manager name that is provided is null.
     *
     * @param schemaManagerName the name of the SchemaManager to load, or null
     */
    private void loadSchemaManager( String schemaManagerName ) {
        if ( !applicationContext.isActive() ) {
            LOG.info( "Restarting context..." );
            applicationContext.refresh();
        }

        if ( schemaManagerName != null ) {
            LOG.info( "Looking up SchemaManager impl: {}", schemaManagerName );
            this.schemaManager = applicationContext.getBean( schemaManagerName, SchemaManager.class );
        }
        else {
            LOG.info( "The SchemaManager is not specified - using the default SchemaManager impl" );
            this.schemaManager = applicationContext.getBean( SchemaManager.class );
        }

        schemaManager.create();
        schemaManager.populateBaseData();
    }


    /**
     * Creates a new instance of the CassandraResource with rpc and storage ports that may or may not be the default
     * ports. If either port is taken, an alternative free port is found.
     *
     * @param schemaManagerName the name of the schemaManager to use
     *
     * @return a new CassandraResource with possibly non-default ports
     */
    public static CassandraResource newWithAvailablePorts( String schemaManagerName ) {
        // Uncomment to test for Surefire Failures
        // System.setSecurityManager( new NoExitSecurityManager( System.getSecurityManager() ) );

        synchronized ( lock ) {
            if ( instance != null ) {
                return instance;
            }

            int rpcPort = AvailablePortFinder
                    .getNextAvailable( CassandraResource.DEFAULT_RPC_PORT + RandomUtils.nextInt( 1000 ) );
            int storagePort = AvailablePortFinder
                    .getNextAvailable( CassandraResource.DEFAULT_STORAGE_PORT + RandomUtils.nextInt( 1000 ) );
            int sslStoragePort = AvailablePortFinder
                    .getNextAvailable( CassandraResource.DEFAULT_SSL_STORAGE_PORT + RandomUtils.nextInt( 1000 ) );
            int nativeTransportPort = AvailablePortFinder
                    .getNextAvailable( CassandraResource.DEFAULT_NATIVE_TRANSPORT_PORT + RandomUtils.nextInt( 1000 ) );

            if ( rpcPort == storagePort ) {
                storagePort++;
                storagePort = AvailablePortFinder.getNextAvailable( storagePort );
            }

            if ( sslStoragePort == storagePort ) {
                sslStoragePort++;
                sslStoragePort = AvailablePortFinder.getNextAvailable( sslStoragePort );
            }

            instance = new CassandraResource( 
                schemaManagerName, rpcPort, storagePort, sslStoragePort, nativeTransportPort );

            LOG.info("Created a new instance of CassandraResource: {}", instance);
            LOG.info("Cassandra using ports {} and {}", storagePort, sslStoragePort);
            return instance;
        }
    }
    public static CassandraResource newWithMavenAllocatedPorts() {
      synchronized ( lock ) {
          Properties props = new Properties();
          try {
        props.load(ClassLoader.getSystemResourceAsStream( "project.properties" ));

          } catch (IOException e) {
        LOG.error("Unable to load project properties: {}", e.getLocalizedMessage());
      }
          int rpcPort = Integer.parseInt(props.getProperty("cassandra.rpcPort", Integer.toString(CassandraResource.DEFAULT_RPC_PORT)));
          int storagePort = Integer.parseInt(props.getProperty("cassandra.storagePort", Integer.toString(CassandraResource.DEFAULT_STORAGE_PORT))) ;
          int sslStoragePort = Integer.parseInt(props.getProperty("cassandra.sslPort", Integer.toString(CassandraResource.DEFAULT_SSL_STORAGE_PORT)));
          int nativeTransportPort = Integer.parseInt(props.getProperty("cassandra.nativeTransportPort", Integer.toString(CassandraResource.DEFAULT_NATIVE_TRANSPORT_PORT)));
          String host = props.getProperty("cassandra.host", DEFAULT_HOST);
//          int rpcPort = CassandraResource.DEFAULT_RPC_PORT;
//          int storagePort = CassandraResource.DEFAULT_STORAGE_PORT ;
//          int sslStoragePort = CassandraResource.DEFAULT_SSL_STORAGE_PORT;
//          int nativeTransportPort = CassandraResource.DEFAULT_NATIVE_TRANSPORT_PORT;

          System.setProperty( "cassandra.url", host+":" + Integer.toString( rpcPort ) );
          System.setProperty( "cassandra.cluster", props.getProperty("cassandra.cluster","Test Cluster") );
          System.setProperty( "cassandra-foreground", "true" );
          System.setProperty( "log4j.defaultInitOverride", "true" );
          System.setProperty( "log4j.configuration", "log4j.properties" );
          System.setProperty( "cassandra.ring_delay_ms", "100" );
          
          System.setProperty( "cassandra." + RPC_PORT_KEY, Integer.toString( rpcPort ) );
          System.setProperty( "cassandra." + STORAGE_PORT_KEY, Integer.toString( storagePort ) );
          System.setProperty( "cassandra." + SSL_STORAGE_PORT_KEY, Integer.toString( sslStoragePort ) );
          System.setProperty( "cassandra." + NATIVE_TRANSPORT_PORT_KEY, Integer.toString( nativeTransportPort ) );

          LOG.info("project.properties loaded properties for ports : "
                  + "[rpc, storage, sslStorage, native] = [{}, {}, {}, {}]", 
                  new Object[] {rpcPort, storagePort, sslStoragePort, nativeTransportPort});


          instance = new CassandraResource( 
              null, rpcPort, storagePort, sslStoragePort, nativeTransportPort );
  
          LOG.info("Created a new instance of CassandraResource: {}", instance);
          LOG.info("Cassandra using ports {} and {}", storagePort, sslStoragePort);
          
          return instance;
      }
    }


    /**
     * Creates a new instance of the CassandraResource with rpc and storage ports that may or may not be the default
     * ports. If either port is taken, an alternative free port is found.
     *
     * @return a new CassandraResource with possibly non-default ports
     */
    public static CassandraResource newWithAvailablePorts() {
        return newWithMavenAllocatedPorts();
    }


    /**
     * Uses a project.properties file that Maven does substitution on to to replace the value of a property with the
     * path to the Maven build directory (a.k.a. target). It then uses this path to generate a random String which it
     * uses to append a path component to so a unique directory is selected. If already present it's deleted, then the
     * directory is created.
     *
     * @return a unique temporary directory
     *
     * @throws IOException if we cannot access the properties file
     */
    public static File getTempDirectory() throws IOException {
        File tmpdir;
        File basedir = new File( ( String ) getProjectProperties().get( TARGET_DIRECTORY_KEY ) );
        String comp = RandomStringUtils.randomAlphanumeric( 7 );
        tmpdir = new File( basedir, comp );

        if ( tmpdir.exists() ) {
            LOG.info( "Deleting directory: {}", tmpdir );
            FileUtils.forceDelete( tmpdir );
        }
        else {
            LOG.info( "Creating temporary directory: {}", tmpdir );
            FileUtils.forceMkdir( tmpdir );
        }

        return tmpdir;
    }


    public static String getTargetDir() throws IOException {
        File basedir = new File( ( String ) getProjectProperties().get( TARGET_DIRECTORY_KEY ) );
        return basedir.getAbsolutePath();
    }


    public static String getJammArgument() throws IOException {
        return ( String ) getProjectProperties().get( JAMM_PATH ); 
    }


    public static Properties getProjectProperties() throws IOException {
        if ( properties == null ) {
            properties = new Properties();
            properties.load( ClassLoader.getSystemResourceAsStream( PROPERTIES_FILE ) );
        } 
        return properties;
    }

}
