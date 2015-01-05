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

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

import org.apache.usergrid.cassandra.CassandraResource;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.lock.MultiProcessBarrier;
import org.apache.usergrid.lock.MultiProcessLocalLock;
import org.apache.usergrid.persistence.index.impl.ElasticSearchResource;


/**
 * Perform the following setup in the system.
 *
 * 1) Inject the properties
 */
public class SystemSetup {

    private static final String TEMP_FILE_PATH = "target/surefirelocks";
    public static final String LOCK_NAME = TEMP_FILE_PATH + "/lock";
    public static final String START_BARRIER_NAME = TEMP_FILE_PATH + "/start_barrier";


//    public static final long ONE_MINUTE = 60000;

    /**
     *
     */
    public static CassandraResource cassandraResource = new CassandraResource();

    public static ElasticSearchResource elasticSearchResource = new ElasticSearchResource();

    public static SpringResource springResource = SpringResource.getInstance();

    private SystemSetup(){

    }


    public void maybeInitialize() throws Exception {

        final MultiProcessLocalLock lock = new MultiProcessLocalLock( LOCK_NAME );

        final MultiProcessBarrier barrier = new MultiProcessBarrier( START_BARRIER_NAME );

        //we have a lock, so init the system
        if ( lock.tryLock() ) {
            initSystem();
            barrier.proceed();
        }


        barrier.await( ONE_MINUTE );

        //it doesn't matter who finishes first.  We need to remove the resources so we start correctly next time.
        //not ideal, but a clean will solve this issue too
        if ( lock.hasLock() ) {
            deleteFile( LOCK_NAME );
            deleteFile( START_BARRIER_NAME );
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


    /**
     * Initialize the system
     */
    public void initSystem() {
        cassandraResource.start();
        elasticSearchResource.start();
        springResource.migrate();
    }
}
