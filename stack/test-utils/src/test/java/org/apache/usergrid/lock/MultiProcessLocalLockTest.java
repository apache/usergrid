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

package org.apache.usergrid.lock;


import java.io.File;
import java.io.IOException;

import org.junit.Test;

import static org.apache.usergrid.TestHelper.newUUIDString;
import static org.junit.Assert.*;


/**
 * Simple test for multiple process lock
 */
public class MultiProcessLocalLockTest {

    public static final int LOCK_PORT = Integer.parseInt( System.getProperty( "test.lock.port", "10101") );

    /**
     * Create and verify the single lock
     * @throws IOException
     */
    @Test
    public void singleLock() throws IOException {

        final String lockName = newFileName();

        MultiProcessLocalLock lock = new MultiProcessLocalLock( LOCK_PORT);

        assertTrue(lock.tryLock());

        assertTrue(lock.hasLock());

        lock.maybeReleaseLock();

    }

    /**
       * Create and verify the single lock
       * @throws IOException
       */
      @Test
      public void multiLock() throws IOException {

          final String lockName = newFileName();

          MultiProcessLocalLock lock1 = new MultiProcessLocalLock( LOCK_PORT );

          assertTrue( lock1.tryLock() );

          assertTrue( lock1.hasLock() );


          //get lock 2, should fail
          MultiProcessLocalLock lock2 = new MultiProcessLocalLock( LOCK_PORT );

          assertFalse(lock2.tryLock());

          assertFalse(lock2.hasLock());


          //release lock1
          boolean lock1release = lock1.maybeReleaseLock();

          assertTrue( "lock released", lock1release );

          boolean lock2release = lock2.maybeReleaseLock();

          assertFalse( "lock released", lock2release );




          //should succeed
          assertTrue(lock2.tryLock());

          assertTrue(lock2.hasLock());

          assertFalse(lock1.tryLock());

          assertFalse(lock1.hasLock());

          lock1release = lock1.maybeReleaseLock();

          assertFalse( "lock released", lock1release );

          lock2release = lock2.maybeReleaseLock();

          assertTrue( "lock released", lock2release );

      }

    private String newFileName() throws IOException {
        return File.createTempFile( "test", "" ).getAbsolutePath();
    }
}


