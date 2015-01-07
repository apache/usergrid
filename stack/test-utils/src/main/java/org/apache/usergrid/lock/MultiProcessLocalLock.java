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
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;


/**
 * A lock that will work across multiple processes and threads on the same machine. This uses The file system to attempt
 * to obtain a lock.  No blocking is performed, the lock is either successful or fails
 */
public class MultiProcessLocalLock {

    private final String fileName;
    private FileLock lock;


    /**
     * The filename to use as the lock
     */
    public MultiProcessLocalLock( final String fileName ) {
        this.fileName = fileName;
    }


    /**
     * Attempts to lock the file.  If a lock cannot be acquired, false is returned.  Otherwise, true is returned.
     *
     * @return true if the lock was acquired.  False otherwise.
     */
    public boolean tryLock() throws IOException {

        if ( lock != null ) {
            throw new IllegalStateException( "You already have a lock, you cannot get a lock again" );
        }

        File file = new File( fileName );

        if ( !file.exists() ) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }

        // get a file channel
        FileChannel fileChannel = new RandomAccessFile( file, "rw" ).getChannel();

        try {
            lock = fileChannel.tryLock();
        }
        //we don't have the lock, ignore
        catch(OverlappingFileLockException ofle){
            return false;
        }


        return hasLock();
    }


    /**
     * Release the lock if we hold it.
     */
    public void releaseLock() throws IOException {
        if ( lock == null ) {
            throw new IllegalStateException( "You cannot release a lock you do not have" );
        }


        lock.release();

        lock = null;
    }


    /**
     * Return true if this instance has the lock
     * @return
     */
    public boolean hasLock(){
        return lock != null;
    }

    /**
     * Releases the lock if we have it, otherwise is a no-op
     * @return
     */
    public void maybeReleaseLock() throws IOException {

        if(lock == null){
            return;
        }

        releaseLock();
    }
}
