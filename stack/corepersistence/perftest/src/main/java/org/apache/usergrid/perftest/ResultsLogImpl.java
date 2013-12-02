package org.apache.usergrid.perftest;

import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


/**
 * An asynchronous results log implementation.
 */
public class ResultsLogImpl implements ResultsLog, Runnable {
    private static final Logger LOG = LoggerFactory.getLogger( ResultsLogImpl.class );

    private final AtomicLong resultCount = new AtomicLong();
    private final AtomicBoolean isOpen = new AtomicBoolean( false );
    private LinkedBlockingDeque<String> buffer = new LinkedBlockingDeque<String>();
    private PrintWriter out;
    private Thread thread;

    private DynamicStringProperty resultsFile;
    private DynamicLongProperty waitTime;


    public ResultsLogImpl() {
        String defaultFile = "/tmp/perftest_results.log";

        resultsFile = DynamicPropertyFactory.getInstance().getStringProperty( RESULTS_FILE_KEY, defaultFile );
        waitTime = DynamicPropertyFactory.getInstance().getLongProperty( WAIT_TIME_KEY, 200 );
    }


    @Override
    public void open() throws IOException {
        synchronized ( isOpen ) {
            if ( isOpen.compareAndSet( false, true ) ) {
                out = new PrintWriter( resultsFile.get() );
                resultCount.set( 0 );
                thread = new Thread( this, "ResultLog Writer" );
                thread.start();
            }
        }
    }


    @Override
    public void close() {
        if ( isOpen.compareAndSet( true, false ) ) {

            // Forces us to wait until the writer thread dies
            synchronized ( isOpen ) {
                out.flush();
                out.close();
                thread = null;
            }
        }
    }


    @Override
    public void truncate() throws IOException {
        if ( isOpen.get() ) {
            throw new IOException( "Cannot truncate while log is open for writing. Close the log then truncate." );
        }

        // Synchronize on isOpen to prevent re-opening while truncating (rare)
        synchronized ( isOpen ) {
            File results = new File( resultsFile.get() );
            FileChannel channel = new FileOutputStream( results, true ).getChannel();
            channel.truncate( 0 );
            channel.close();
            resultCount.set( 0 );
        }
    }


    @Override
    public void write( String result ) {
        try {
            buffer.putFirst( result );
        }
        catch ( InterruptedException e ) {
            LOG.error( "Was interrupted on write.", e );
        }
    }


    @Override
    public long getResultCount() {
        return resultCount.get();
    }


    @Override
    public String getPath() {
        return resultsFile.get();
    }


    @Override
    public void run() {
        synchronized ( isOpen )
        {
            // Keep writing after closed until buffer is flushed (empty)
            while ( isOpen.get() || ! buffer.isEmpty() ) {
                try {
                    String result = buffer.pollLast( waitTime.get(), TimeUnit.MILLISECONDS );

                    if ( result != null ) {
                        resultCount.incrementAndGet();
                        out.println( result );
                    }
                }
                catch ( InterruptedException e ) {
                    LOG.error( "ResultLog thread interrupted.", e );
                }
            }

            isOpen.notifyAll();
        }
    }
}
