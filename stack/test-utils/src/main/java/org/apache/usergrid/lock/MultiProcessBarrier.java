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
import java.util.concurrent.TimeoutException;


/**
 * A barrier between processes and threads. Everyone will await until proceed has been invoked by a single
 * thread.  Other threads will proceed after wait time.
 */
public class MultiProcessBarrier {

    /**
     * The sleep time to wait before checking.
     */
    private static final long SLEEP_TIME = 100;
    public final File barrierFile;


    public MultiProcessBarrier( final String barrierFileName ) {
        this.barrierFile = new File( barrierFileName );
    }


    /**
     * Notify the other processes they can proceed.
     */
    public void proceed() throws IOException {
        barrierFile.mkdirs();
        barrierFile.createNewFile();
    }


    /**
     * Await the specified file.  If it exists, it will proceed
     * @param timeout
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public void await(final long timeout) throws InterruptedException, TimeoutException {

        final long stopTime = System.currentTimeMillis() + timeout;

        while(System.currentTimeMillis() < stopTime){

            //barrier is done break
            if(barrierFile.exists()){
                return;
            }

            Thread.sleep( SLEEP_TIME );
        }

        throw new TimeoutException( "Timeout out after " + timeout + " milliseconds waiting for the file" );
    }
}
