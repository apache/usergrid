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


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeoutException;


/**
 * A barrier between processes and threads. Everyone will await until proceed has been invoked by a single thread.
 * Other threads will proceed after wait time.
 */
public class MultiProcessBarrier {

    /**
     * The sleep time to wait before checking.
     */
    private static final int SLEEP_TIME = 100;
    public final int barrierPort;
    public ServerSocket serverSocket;


    public MultiProcessBarrier( final int barrierPort ) {
        this.barrierPort = barrierPort;
    }


    /**
     * Notify the other processes they can proceed.
     */
    public void proceed() throws IOException {
        serverSocket = new ServerSocket( barrierPort );
    }


    /**
     * Await the specified file.  If it exists, it will proceed
     */
    public void await( final long timeout ) throws InterruptedException, TimeoutException {

        final long stopTime = System.currentTimeMillis() + timeout;

        while ( System.currentTimeMillis() < stopTime ) {


            try {
                Socket client = new Socket();
                client.connect( new InetSocketAddress( "127.0.0.1", barrierPort ), SLEEP_TIME );
                //if we get here we're good, the client can connect and close
                client.close();

                finalize();

                return;
            }
            catch ( IOException e ) {
                //not open swallow and retry
            }
            catch ( Throwable throwable ) {
                throw new RuntimeException( "Something unexpected happened", throwable );
            }
        }

        throw new TimeoutException( "Timeout out after " + timeout + " milliseconds waiting for the file" );
    }


    @Override
    protected void finalize() throws Throwable {
        if ( serverSocket != null && !serverSocket.isClosed() ) {
            serverSocket.close();
        }
    }
}
