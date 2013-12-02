package org.apache.usergrid.perftest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Tests the ResultsLogImpl.
 */
public class ResultsLogImplTest {
    private static final Logger LOG = LoggerFactory.getLogger( ResultsLogImplTest.class );
    private final AtomicLong resultCount = new AtomicLong();
    private final ResultsLog resultsLog = new ResultsLogImpl();
    private ExecutorService executorService;
    private Runnable runnable;


    @Before
    public void setup() throws IOException {
        LOG.info( "Setting up with log file {}.", resultsLog.getPath() );
        executorService = Executors.newFixedThreadPool( 10 );
        resultsLog.open();
        runnable = new Runnable() {
            @Override
            public void run() {
                for ( int ii = 0; ii < 1000; ii++ ) {
                    resultsLog.write( "we are writing out a damn record" );
                    resultCount.incrementAndGet();
                }
            }
        };
    }


    @After
    public void tearDown() throws InterruptedException {
        if ( ! executorService.isShutdown() || ! executorService.isTerminated() )
        {
            executorService.shutdown();
            executorService.awaitTermination( 1000, TimeUnit.MILLISECONDS );
            executorService = null;
        }

        resultsLog.close();
        resultCount.set( 0 );
    }


    @Test
    public void testLog() throws InterruptedException, IOException {
        for ( int ii = 0; ii < 100; ii++ )
        {
            executorService.execute( runnable );
        }

        executorService.awaitTermination( 1000, TimeUnit.MILLISECONDS );
        resultsLog.close();
        File file = new File( resultsLog.getPath() );
        assertTrue( file.exists() );
        assertTrue( file.length() > 100 );
        assertEquals( resultCount.get(), resultsLog.getResultCount() );
        LOG.info( "The length of the result file is {} bytes.", file.length() );

        resultsLog.truncate();
        file = new File( resultsLog.getPath() );
        LOG.info("The length of the result file after truncating is {} bytes.", file.length());
        assertTrue( file.length() == 0 );
    }
}
