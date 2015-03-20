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

package org.apache.usergrid.setup;


import org.apache.usergrid.cassandra.SchemaManager;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.lock.MultiProcessBarrier;
import org.apache.usergrid.lock.MultiProcessLocalLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


/**
 * A singleton that starts cassandra and configures it once per JVM
 */
public class ConcurrentProcessSingleton {

    private static final Logger logger = LoggerFactory.getLogger( ConcurrentProcessSingleton.class );

    private static final String TEMP_FILE_PATH = "target/surefirelocks/start_barrier-"
        + System.getProperty( "test.barrier.timestamp", "default" );

    public static final int LOCK_PORT = Integer.parseInt(
        System.getProperty( "test.lock.port", "10101" ) );

    public static final boolean CLEAN_STORAGE =
        Boolean.parseBoolean( System.getProperty( "test.clean.storage", "false" ) );


    public static final long ONE_MINUTE = 60000;


    private final MultiProcessLocalLock lock = new MultiProcessLocalLock( LOCK_PORT );
    private final MultiProcessBarrier barrier = new MultiProcessBarrier( TEMP_FILE_PATH );


    private static ConcurrentProcessSingleton instance;

    private SpringResource springResource;


    /**
     * Create the default instance
     */
    private ConcurrentProcessSingleton() {
        springResource = SpringResource.getInstance();
    }


    public SpringResource getSpringResource() {
        assert springResource != null;
        return springResource;
    }


    private void startSystem() {
        try {

            logger.info( "Trying to get a lock to setup system" );

            // we have a lock, so init the system
            if ( lock.tryLock() ) {

                logger.info( "Lock acquired, setting up system" );

                final SchemaManager schemaManager =
                    SpringResource.getInstance().getBean( SchemaManager.class );

                // maybe delete existing column families and indexes
                if ( CLEAN_STORAGE ) {
                    logger.info("Destroying current database");
                    schemaManager.destroy();
                }

                // create our schema
                logger.info("Creating database");
                schemaManager.create();

                logger.info("Populating database");
                schemaManager.populateBaseData();

                // signal to other processes we've migrated, and they can proceed
                barrier.proceed();
            }


            logger.info( "Waiting for setup to complete" );
            barrier.await( ONE_MINUTE );
            logger.info( "Setup to complete" );

            Runtime.getRuntime().addShutdownHook( new Thread(  ){
                @Override
                public void run() {
                    try {
                        lock.maybeReleaseLock();
                    }
                    catch ( IOException e ) {
                        throw new RuntimeException( "Unable to release lock" );
                    }
                }
            });

        }
        catch ( Exception e ) {
            throw new RuntimeException( "Unable to initialize system", e );
        }
    }


    /**
     * Get an instance of this singleton.  If it is the first time this instance is
     * created it will also initialize the system
     */
    public static synchronized ConcurrentProcessSingleton getInstance() {
        if ( instance != null ) {
            return instance;
        }

        instance = new ConcurrentProcessSingleton();
        instance.startSystem();

        return instance;
    }
}
