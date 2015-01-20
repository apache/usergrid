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


    public static final int LOCK_PORT = Integer.parseInt( System.getProperty( "test.lock.port", "10101") );
    public static final int START_BARRIER_PORT =  Integer.parseInt( System.getProperty( "test.barrier.port", "10102") );


    public static final long ONE_MINUTE = 60000;

    final MultiProcessLocalLock lock = new MultiProcessLocalLock( LOCK_PORT );
    final MultiProcessBarrier barrier = new MultiProcessBarrier( START_BARRIER_PORT );


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

        lock.maybeReleaseLock();
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
