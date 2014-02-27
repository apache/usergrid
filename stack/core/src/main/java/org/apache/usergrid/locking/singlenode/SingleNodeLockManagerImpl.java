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
package org.apache.usergrid.locking.singlenode;


import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.locking.Lock;
import org.apache.usergrid.locking.LockManager;
import org.apache.usergrid.locking.LockPathBuilder;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;


/**
 * Single Node implementation for {@link LockManager} Note that this implementation has not been used in a production
 * environment.
 * <p/>
 * The hector based implementation is the preferred production locking system
 */
public class SingleNodeLockManagerImpl implements LockManager {

    private static final Logger logger = LoggerFactory.getLogger( SingleNodeLockManagerImpl.class );

    public static final long MILLI_EXPIRATION = 5000;

    /** Lock cache that sill expire after 5 seconds of no use for a lock path */
    private LoadingCache<String, ReentrantLock> locks =
            CacheBuilder.newBuilder().expireAfterWrite( MILLI_EXPIRATION, TimeUnit.MILLISECONDS )
                    // use weakValues. We want want entries removed if they're not being
                    // referenced by another
                    // thread somewhere and GC occurs
                    .weakValues().removalListener( new RemovalListener<String, ReentrantLock>() {

                @Override
                public void onRemoval( RemovalNotification<String, ReentrantLock> notification ) {
                    logger.debug( "Evicting reentrant lock for {}", notification.getKey() );
                }
            } ).build( new CacheLoader<String, ReentrantLock>() {

                @Override
                public ReentrantLock load( String arg0 ) throws Exception {
                    return new ReentrantLock( true );
                }
            } );


    /** Default constructor. */
    public SingleNodeLockManagerImpl() {
    }


    /*
   * (non-Javadoc)
   * 
   * @see org.apache.usergrid.locking.LockManager#createLock(java.util.UUID,
   * java.lang.String[])
   */
    @Override
    public Lock createLock( UUID applicationId, String... path ) {

        String lockPath = LockPathBuilder.buildPath( applicationId, path );

        try {
            return new SingleNodeLockImpl( locks.get( lockPath ) );
        }
        catch ( ExecutionException e ) {
            throw new RuntimeException( "Unable to create lock in cache", e );
        }
    }
}
