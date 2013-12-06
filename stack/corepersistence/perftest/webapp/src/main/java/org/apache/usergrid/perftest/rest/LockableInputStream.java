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


    /**
     * Creates a FileInputStream that blocks near the specified byte count limit only
     * to be unblocked after a call to deactivateLimit().
     *
     * @param file the file to read
     * @param limit the limit in bytes to block at
     * @throws FileNotFoundException
     */
    public LockableInputStream( File file, long limit ) throws FileNotFoundException {
        super( file );
        this.limit = limit;
    }


    /**
     * Used internally to block (trap) all read method calls.
     *
     * @param nextAdvance the amount of bytes the read call was about to advance before
     *                    getting trapped by this block call
     */
    private void block( int nextAdvance ) {
        boolean nextAdvanceHitsLimit = ( readCount + nextAdvance ) >= limit;
        boolean overLimit = readCount > limit;

        if ( overLimit && ( ! limitActive.get() ) ) {
            return;
        }

        // not blocks or over the limit yet but next advance will do so (toggle blocked here)
        if ( nextAdvanceHitsLimit && ( ! overLimit ) && ( ! blocked.get() ) ) {
            synchronized ( lock ) {
                blocked.compareAndSet( false, true );
                lock.notify();
            }

            LOG.info( "{} bytes have been read. About to hit the {} byte limit. " +
                    "The stream will now block until unlocked.", readCount, limit );
        }

        if ( nextAdvanceHitsLimit && ( ! overLimit ) && blocked.get() ) {
            LOG.info( "Next advance will reach the limit: blocking the thread {}", Thread.currentThread() );

            while ( limitActive.get() ) {
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
            LOG.info( "The limit has been deactivated. Reads are allowed to continue." );
        }
    }


    /**
     * Any calling Thread will block on this method until this LockableInputStream reads
     * to the limit where it must block. This is useful for having another thread join
     * this point.
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


    /**
     * Checks to see if this stream is currently blocked at it's limit which is still
     * active.
     *
     * @return true if blocked, false otherwise
     */
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


    /**
     * Gets the current read count, meaning the number of bytes this stream has read
     * since it was opened.
     *
     * @return the number of bytes currently read
     */
    public long getReadCount() {
        return readCount;
    }


    @Override
    public int read() throws IOException {
        block( 1 );
        int ii = super.read();
        readCount++;
        return ii;
    }


    @Override
    public int read( byte[] b ) throws IOException {
        if ( b != null ) {
            block( b.length );
        }

        int ii = super.read( b );
        readCount += ii;
        return ii;
    }


    @Override
    public int read( byte[] b, int off, int length ) throws IOException {
        block( length );

        int ii = super.read( b, off, length );
        readCount += ii;
        return ii;
    }
}
