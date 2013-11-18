package org.usergrid;


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
