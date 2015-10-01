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
package org.apache.usergrid.launcher;


/** @author Ran Tavory (rantav@gmail.com) */
public class EmbeddedServerHelper {
    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( EmbeddedServerHelper.class );

    private static final String TMP = "tmp";

    private final String yamlFile;
    static org.apache.cassandra.service.CassandraDaemon cassandraDaemon;


    public EmbeddedServerHelper() {
        this( "/cassandra.yaml" );
    }


    public EmbeddedServerHelper( String yamlFile ) {
        this.yamlFile = yamlFile;
    }


    static java.util.concurrent.ExecutorService executor;


    /** Set embedded cassandra up and spawn it in a new thread. */
    public void setup() throws org.apache.thrift.transport.TTransportException, java.io.IOException, InterruptedException {

        // delete tmp dir first
        rmdir( TMP );
        // make a tmp dir and copy cassandra.yaml and log4j.properties to it
        copy( "/log4j-server.properties", TMP );
        copy( yamlFile, TMP );

        System.setProperty( "cassandra.config", "file:" + TMP + yamlFile );
        System.setProperty( "log4j.configuration", "file:" + TMP + "/log4j-server.properties" );
        System.setProperty( "cassandra-foreground", "true" );

        cleanupAndLeaveDirs();
        loadSchemaFromYaml();
        // loadYamlTables();
    }


    public void start() throws org.apache.thrift.transport.TTransportException, java.io.IOException, InterruptedException {
        if ( executor == null ) {
            executor = java.util.concurrent.Executors.newSingleThreadExecutor();
            System.setProperty( "cassandra.config", "file:" + TMP + yamlFile );
            System.setProperty( "log4j.configuration", "file:" + TMP + "/log4j.properties" );
            System.setProperty( "cassandra-foreground", "true" );

            log.info( "Starting executor" );

            executor.execute( new ITRunner() );
            log.info( "Started executor" );
        }
        else {
            cassandraDaemon.start();
        }


        try {
            java.util.concurrent.TimeUnit.SECONDS.sleep( 3 );
            log.info( "Done sleeping" );
        }
        catch ( InterruptedException e ) {
            throw new AssertionError( e );
        }
    }


    public static void teardown() {
        if ( cassandraDaemon != null ) {
            cassandraDaemon.deactivate();
            org.apache.cassandra.service.StorageService.instance.stopClient();
        }
        executor.shutdown();
        executor.shutdownNow();
        log.info( "Teardown complete" );
    }


    public void stop() {
        cassandraDaemon.stop();
    }


    private static void rmdir( String dir ) throws java.io.IOException {
        java.io.File dirFile = new java.io.File( dir );
        if ( dirFile.exists() ) {
            org.apache.cassandra.io.util.FileUtils.deleteRecursive( new java.io.File( dir ) );
        }
    }


    /** Copies a resource from within the jar to a directory. */
    private static void copy( String resource, String directory ) throws java.io.IOException {
        mkdir( directory );
        java.io.InputStream is = EmbeddedServerHelper.class.getResourceAsStream( resource );
        String fileName = resource.substring( resource.lastIndexOf( "/" ) + 1 );
        java.io.File file = new java.io.File( directory + System.getProperty( "file.separator" ) + fileName );
        java.io.OutputStream out = new java.io.FileOutputStream( file );
        byte buf[] = new byte[1024];
        int len;
        while ( ( len = is.read( buf ) ) > 0 ) {
            out.write( buf, 0, len );
        }
        out.close();
        is.close();
    }


    /** Creates a directory */
    private static void mkdir( String dir ) throws java.io.IOException {
        org.apache.cassandra.io.util.FileUtils.createDirectory( dir );
    }


    public static void cleanupAndLeaveDirs() throws java.io.IOException {
        mkdirs();
        cleanup();
        mkdirs();
        org.apache.cassandra.db.commitlog.CommitLog.instance.resetUnsafe(); // cleanup screws w/ CommitLog, this
        // brings it back to safe state
    }


    public static void cleanup() throws java.io.IOException {
        // clean up commitlog
        String[] directoryNames = { org.apache.cassandra.config.DatabaseDescriptor.getCommitLogLocation(), };
        for ( String dirName : directoryNames ) {
            java.io.File dir = new java.io.File( dirName );
            if ( !dir.exists() ) {
                throw new RuntimeException( "No such directory: " + dir.getAbsolutePath() );
            }
            org.apache.cassandra.io.util.FileUtils.deleteRecursive( dir );
        }

        // clean up data directory which are stored as data directory/table/data
        // files
        for ( String dirName : org.apache.cassandra.config.DatabaseDescriptor.getAllDataFileLocations() ) {
            java.io.File dir = new java.io.File( dirName );
            if ( !dir.exists() ) {
                throw new RuntimeException( "No such directory: " + dir.getAbsolutePath() );
            }
            org.apache.cassandra.io.util.FileUtils.deleteRecursive( dir );
        }
    }


    public static void mkdirs() {
        org.apache.cassandra.config.DatabaseDescriptor.createAllDirectories();
    }


    public static void loadSchemaFromYaml() {
        try {
            me.prettyprint.hector.testutils.EmbeddedSchemaLoader.loadSchema();
        }
        catch ( RuntimeException e ) {

        }
    }


    class ITRunner implements Runnable {

        @Override
        public void run() {

            cassandraDaemon = new org.apache.cassandra.service.CassandraDaemon();

            cassandraDaemon.activate();
        }
    }
}
