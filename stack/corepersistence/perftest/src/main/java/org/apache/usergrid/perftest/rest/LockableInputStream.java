package org.apache.usergrid.perftest.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A FileInputStream subclass that wraps super calls with checks to see if we could
 * potentially hit some limit where we want to lock the stream: that is make it block
 * on subsequent reads. This class is used to stop just before transmitting a war
 * file to the Tomcat Manager on a /load REST operation. This allows us to schedule
 * unlocking this stream in a Runner after returning to the client from a REST call.
 * This helps us avoid a highly probable race condition between a load response and
 * the reload of the web application.
 */
class LockableInputStream extends FileInputStream {
    private static final Logger LOG = LoggerFactory.getLogger( LockableInputStream.class );

    private final long limit;
    private final AtomicBoolean limitActive = new AtomicBoolean( true );
    private final AtomicBoolean blocked = new AtomicBoolean( false );
    private final Object lock = new Object();
    private long readCount = 0;

    public LockableInputStream( File file, long limit ) throws FileNotFoundException {
        super( file );
        this.limit = limit;
    }

    private void block( int nextAdvance ) {
        boolean atLimit = readCount >= ( limit - nextAdvance );

        if ( atLimit ) {
            synchronized ( lock ) {
                blocked.compareAndSet( false, true );
                lock.notify();
            }

            LOG.info( "{} bytes have been read. About to hit the {} byte limit. " +
                    "The stream will now block until unlocked.", readCount, limit );
        }
        else {
            return;
        }

        while ( atLimit && limitActive.get() )
        {
            synchronized ( lock ) {
                try {
                    lock.wait( 100 );
                    lock.notify();
                }
                catch ( InterruptedException e ) {
                    LOG.error( "Interrupted while waiting in the read() method." );
                }
            }
        }

        LOG.info( "It seems the limit has been deactivated. Reads will now continue." );
    }


    /**
     * Calling Thread will block until this stream reads to the limit where it blocks.
     *
     * @throws InterruptedException
     */
    public void returnOnLimit() throws InterruptedException {
        while ( ! blocked.get() )
        {
            synchronized ( lock ) {
                lock.wait( 250 );
                lock.notify();
            }
        }
    }

    public boolean isBlockedAtLimit() {
        return blocked.get();
    }

    public void deactivateLimit() {
        synchronized ( lock ) {
            limitActive.compareAndSet( true, false );
            blocked.compareAndSet( true, false );
            lock.notifyAll();
        }
    }

    public long getReadCount() {
        return readCount;
    }

    public int read() throws IOException {
        block( 1 );
        int ii = super.read();
        readCount++;
        return ii;
    }

    public int read( byte[] b ) throws IOException {
        if ( b != null ) {
            block( b.length );
        }

        int ii = super.read( b );
        readCount += ii;
        return ii;
    }

    public int read( byte[] b, int off, int length ) throws IOException {
        block( length );

        int ii = super.read( b, off, length );
        readCount += ii;
        return ii;
    }
}
