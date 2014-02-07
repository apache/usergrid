package org.apache.usergrid.persistence.collection.rx;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.hystrix.CassandraCommand;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.netflix.config.ConfigurationManager;

import rx.Observable;
import rx.util.functions.Func1;
import rx.util.functions.FuncN;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 *
 *
 */
public class ConcurrentTest {

    private static final Logger logger = LoggerFactory.getLogger( ConcurrentTest.class );


    private int preRunThreadCount = -1;
    private int queueDepth = -1;


    @Before
    public void getSettings() {

        //        preRunThreadCount = ConfigurationManager.getConfigInstance().getInt( CassandraCommand
        // .THREAD_POOL_SIZE );
        //
        //
        //        //reject requests we have to queue
        //        queueDepth = ConfigurationManager.getConfigInstance().getInt( CassandraCommand.THREAD_POOL_QUEUE );
    }


    @After
    public void restoreSettings() {
        ConfigurationManager.getConfigInstance().setProperty( CassandraCommand.THREAD_POOL_SIZE, preRunThreadCount );


        //reject requests we have to queue
        ConfigurationManager.getConfigInstance().setProperty( CassandraCommand.THREAD_POOL_QUEUE, queueDepth );
    }


    @Test( timeout = 5000 )
    public void concurrent() {

        final String source = "test";
        Observable<String> observable = CassandraCommand.toObservable( source );

        //we could theoretically use the same instance over and over

        final int size = 100;

        //set the size of concurrent threads to our requests/2.  Should be more than sufficient for this test
        ConfigurationManager.getConfigInstance().setProperty( CassandraCommand.THREAD_POOL_SIZE, size / 2 );


        //reject requests we have to queue
        ConfigurationManager.getConfigInstance().setProperty( CassandraCommand.THREAD_POOL_QUEUE, -1 );


        final CountDownLatch latch = new CountDownLatch( size );

        TestConcurrent[] concurrentFunctions = new TestConcurrent[size];

        for ( int i = 0; i < size; i++ ) {
            concurrentFunctions[i] = new TestConcurrent( latch, i );
        }

        Zip zip = new Zip();


        //concurrent inherits thread pool from it's observable, set it's thread pool
        Observable<Integer> result = Concurrent.concurrent( observable, zip, concurrentFunctions );

        assertEquals( "No invocation yet", 0, set.size() );


        //now invoke it
        Integer response = result.toBlockingObservable().single();


        assertEquals( "Same value emitted", size - 1, response.intValue() );

        //verify each function executed in it's own thread

        assertEquals( size + " threads invoked", size, set.size() );

        //print them out just for giggles
        for ( Multiset.Entry<String> entry : set.entrySet() ) {
            System.out.println( entry.getElement() );
            assertEquals( "1 Thread per invocation", 1, entry.getCount() );
        }
    }


    private Multiset<String> set = HashMultiset.create();


    public class Zip implements FuncN<Integer> {


        @Override
        public Integer call( final Object... args ) {

            //validate our arguments come in order
            for ( int i = 0; i < args.length; i++ ) {

                assertEquals( i, args[i] );
            }

            return ( Integer ) args[args.length - 1];
        }
    }


    /**
     * Simple function that just adds data to our multiset for later evaluation
     */
    public class TestConcurrent implements Func1<String, Integer> {

        private final CountDownLatch latch;
        private final int index;


        public TestConcurrent( final CountDownLatch latch, int index ) {
            this.latch = latch;
            this.index = index;
        }


        @Override
        public Integer call( final String s ) {
            final String threadName = Thread.currentThread().getName();

            logger.info( "Function executing on thread {}", threadName );

            set.add( threadName );

            //we want to make sure each thread blocks until they all have passed the latch
            //this way we can ensure they're all running concurrently
            try {
                latch.countDown();


                boolean waited = latch.await( 5, TimeUnit.SECONDS );

                //validate everything ran concurrently
                assertTrue( "Latch released", waited );
            }
            catch ( InterruptedException e ) {
                logger.error( "Runner interrupted", e );
            }

            return index;
        }
    }
}
