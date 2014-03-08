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
package org.apache.usergrid;


import java.security.Permission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Used to prevent System.exit() calls when testing funky Cassandra exit code and race conditions in the ForkedBooter
 * from Maven's Surefire plugin. Use this as a tool to find out where some issues may arise.
 */
public class NoExitSecurityManager extends java.rmi.RMISecurityManager {
    private static final Logger LOG = LoggerFactory.getLogger( NoExitSecurityManager.class );

    private final SecurityManager parent;


    public NoExitSecurityManager( final SecurityManager manager ) {
        parent = manager;
    }


    @Override
    public void checkExit( int status ) {
        if ( status == 0 ) {
            return;
        }

        Thread thread = Thread.currentThread();

        try {
            thread.sleep( 100L );
        }
        catch ( InterruptedException e ) {
            LOG.error( "failed to sleep", e );
        }


        throw new AttemptToExitException( status );
    }


    @Override
    public void checkPermission( Permission perm ) {
    }


    class AttemptToExitException extends RuntimeException {
        final int status;


        AttemptToExitException( int status ) {
            super( "Exit status = " + status );
            this.status = status;
        }


        public int getStatus() {
            return status;
        }
    }
}
