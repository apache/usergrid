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


import java.io.File;

import org.apache.usergrid.cassandra.CassandraResource;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.lock.MultiProcessBarrier;
import org.apache.usergrid.lock.MultiProcessLocalLock;
import org.apache.usergrid.persistence.index.impl.ElasticSearchResource;


/**
 * Perform the following setup in the system.
 *
 * 1) Inject the properties for Cassandra 2) Inject the properties for Elastic Search 3) Initialize spring 4) If we
 * obtain the lock, we then initialize cassandra. 5) After initialization, we proceed.
 */
public class SystemSetup {

    private static final String TEMP_FILE_PATH = "target/surefirelocks";
    public static final String LOCK_NAME = TEMP_FILE_PATH + "/lock";
    public static final String START_BARRIER_NAME = TEMP_FILE_PATH + "/start_barrier";


    public static final long ONE_MINUTE = 60000;

    final MultiProcessLocalLock lock = new MultiProcessLocalLock( LOCK_NAME );
    final MultiProcessBarrier barrier = new MultiProcessBarrier( START_BARRIER_NAME );


    /**
     * Use the file system to create a multi process lock.  If we have the lock, perform the initialization of the
     * system.
     */
    public void maybeInitialize() throws Exception {


        //we have a lock, so init the system
        if ( lock.tryLock() ) {

            //wire up cassandra and elasticsearch before we start spring, otherwise this won't work
            new CassandraResource().start();

            new ElasticSearchResource().start();

            SpringResource.getInstance().migrate();

            //signal to other processes we've migrated, and they can proceed
            barrier.proceed();
        }


        barrier.await( ONE_MINUTE );

        //it doesn't matter who finishes first.  We need to remove the resources so we start correctly next time.
        //not ideal, but a clean will solve this issue too
        if ( lock.hasLock() ) {

            //add a shutdown hook so we clean up after ourselves.  Kinda fugly, but works since we can't clean on start
            Runtime.getRuntime().addShutdownHook( new Thread() {

                public void run() {

                    System.out.println( "Shutdown Hook is running !" );
                    deleteFile( LOCK_NAME );
                    deleteFile( START_BARRIER_NAME );
                }
            } );


            lock.releaseLock();
        }
    }


    /**
     * Delete the files after we start
     */
    private void deleteFile( final String fileName ) {
        File file = new File( fileName );

        if ( file.exists() ) {
            file.delete();
        }
    }
}
