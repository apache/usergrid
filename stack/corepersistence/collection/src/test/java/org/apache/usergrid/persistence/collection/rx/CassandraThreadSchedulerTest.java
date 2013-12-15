package org.apache.usergrid.persistence.collection.rx;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicIntProperty;

import rx.Scheduler;
import rx.util.functions.Action0;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Test for our scheduler
 */
public class CassandraThreadSchedulerTest {


    private static final Logger LOG = LoggerFactory.getLogger( CassandraThreadSchedulerTest.class );


    @Test
    public void testMaxLimit() throws InterruptedException {

        final int maxCount = 10;

        //kind of a hack, but necessary with the way properties are singletons.  Otherwise you get side effects from
        // other tests
        ConfigurationManager.getConfigInstance().setProperty( CassandraThreadScheduler.RX_IO_THREADS, "" + maxCount );

        final DynamicIntProperty maxThreads =
                new DynamicIntProperty( CassandraThreadScheduler.RX_IO_THREADS, maxCount );

        final CassandraThreadScheduler cassSchedulerSetup = new CassandraThreadScheduler( maxThreads );

        final Scheduler rxScheduler = cassSchedulerSetup.get();

        //we want a fair semaphore so we can release in acquire order
        final Semaphore semaphore = new Semaphore( 0, true );

        //we should not have maxCount actions running in the scheduler
        CountDownLatch result = schedule( rxScheduler, maxThreads.get(), semaphore );

        //schedule and we should fail

        try {

            rxScheduler.schedule( new Action0() {
                @Override
                public void call() {
                    //no op
                }
            } );

            fail( "This should have thrown an exception" );
        }
        catch ( RejectedExecutionException ree ) {
            //swallow, we just want to ensure we catch this to continue the test
        }

        //now release the semaphore so all 10 can run
        semaphore.release( maxThreads.get() );

        //wait for completion
        boolean completed = result.await( 20, TimeUnit.SECONDS );

        assertTrue( "Completed executing actions", completed );

        //verify we can schedule and execute a new operation
        result = schedule( rxScheduler, 1, semaphore );

        semaphore.release( 1 );

        completed = result.await( 20, TimeUnit.SECONDS );

        assertTrue( "Completed executing actions", completed );
    }


    /**
     * Test running from a max limit to a lower limit and fails to schedule new threads
     */
    @Test
    public void testMaxLimitShrink() throws InterruptedException {

        final int maxCount = 10;

        final int half = maxCount / 2;

        //kind of a hack, but necessary with the way properties are singletons.  Otherwise you get side effects from
        // other tests
        ConfigurationManager.getConfigInstance().setProperty( CassandraThreadScheduler.RX_IO_THREADS, "" + maxCount );

        final DynamicIntProperty maxThreads =
                new DynamicIntProperty( CassandraThreadScheduler.RX_IO_THREADS, maxCount );

        final CassandraThreadScheduler cassSchedulerSetup = new CassandraThreadScheduler( maxThreads );

        final Scheduler rxScheduler = cassSchedulerSetup.get();

        //we want a fair semaphore so we can release in acquire order
        final Semaphore semaphore = new Semaphore( 0, true );

        //we should not have maxCount actions running in the scheduler
        CountDownLatch firstHalf = schedule( rxScheduler, half, semaphore );

        //schedule the second half
        CountDownLatch secondHalf = schedule( rxScheduler, half, semaphore );

        //schedule and we should fail

        try {

            rxScheduler.schedule( new Action0() {
                @Override
                public void call() {
                    //no op
                }
            } );

            fail( "This should have thrown an exception" );
        }
        catch ( RejectedExecutionException ree ) {
            //swallow, we just want to ensure we catch this to continue the test
        }


        //update the property to shrink the size
        ConfigurationManager.getConfigInstance().setProperty( CassandraThreadScheduler.RX_IO_THREADS, "" + half );

        //now release the first half of executors
        semaphore.release( half );


        //wait for completion
        boolean completed = firstHalf.await( 20, TimeUnit.SECONDS );

        assertTrue( "Completed executing actions", completed );

        //verify we can't schedule b/c we're still at capacity


        try {

            rxScheduler.schedule( new Action0() {
                @Override
                public void call() {
                    //no op
                }
            } );

            fail( "This should have thrown an exception.  We still don't have capacity for new threads" );
        }
        catch ( RejectedExecutionException ree ) {
            //swallow, we just want to ensure we catch this to continue the test
        }


        //now release the rest of the semaphores
        semaphore.release( maxCount - half );

        completed = secondHalf.await( 20, TimeUnit.SECONDS );

        assertTrue( "Completed executing actions", completed );

        //verify we can schedule and execute a new operation
        CountDownLatch newJob = schedule( rxScheduler, 1, semaphore );

        semaphore.release( 1 );

        completed = newJob.await( 20, TimeUnit.SECONDS );
        assertTrue( "Completed executing actions", completed );
    }


    /**
     * Test that when we're fully blocked, if we expand we have capacity
     */
    @Test
    public void testExpandLimit() throws InterruptedException {

        final int startCount = 10;

        //kind of a hack, but necessary with the way properties are singletons.  Otherwise you get side effects from
        // other tests
        ConfigurationManager.getConfigInstance().setProperty( CassandraThreadScheduler.RX_IO_THREADS, "" + startCount );


        final DynamicIntProperty maxThreads =
                new DynamicIntProperty( CassandraThreadScheduler.RX_IO_THREADS, startCount );

        final CassandraThreadScheduler cassSchedulerSetup = new CassandraThreadScheduler( maxThreads );

        final Scheduler rxScheduler = cassSchedulerSetup.get();

        //we want a fair semaphore so we can release in acquire order
        final Semaphore semaphore = new Semaphore( 0, true );

        //we should not have maxCount actions running in the scheduler
        CountDownLatch firstBatch = schedule( rxScheduler, maxThreads.get(), semaphore );

        //schedule and we should fail

        try {

            rxScheduler.schedule( new Action0() {
                @Override
                public void call() {
                    //no op
                }
            } );

            fail( "This should have thrown an exception" );
        }
        catch ( RejectedExecutionException ree ) {
            //swallow, we just want to ensure we catch this to continue the test
        }


        //now allocate more capacity
        final int doubleMaxCount = startCount * 2;

        //update the property to shrink the size
        ConfigurationManager.getConfigInstance()
                            .setProperty( CassandraThreadScheduler.RX_IO_THREADS, "" + doubleMaxCount );


        //now schedule 10 more

        CountDownLatch secondBatch = schedule( rxScheduler, maxThreads.get() - startCount, semaphore );

        //this should fail.  We're at capacity

        try {

            rxScheduler.schedule( new Action0() {
                @Override
                public void call() {
                    //no op
                }
            } );

            fail( "This should have thrown an exception" );
        }
        catch ( RejectedExecutionException ree ) {
            //swallow, we just want to ensure we catch this to continue the test
        }


        //now release the semaphores so all
        semaphore.release( maxThreads.get() );

        //wait for completion
        boolean completed = firstBatch.await( 20, TimeUnit.SECONDS );

        assertTrue( "Completed executing actions", completed );

        completed = secondBatch.await( 20, TimeUnit.SECONDS );

        assertTrue( "Completed executing actions", completed );

        //verify we can schedule and execute a new operation
        CountDownLatch result = schedule( rxScheduler, 1, semaphore );

        semaphore.release( 1 );

        completed = result.await( 20, TimeUnit.SECONDS );

        assertTrue( "Completed executing actions", completed );
    }


    /**
     * Schedule actions into the semaphore.
     *
     * @param rxScheduler the Scheduler to use
     * @param totalCount The total count of actions to invoke
     * @param semaphore The semaphore to take on acquire
     *
     * @return The latch to block on.  When all jobs have been executed this will be tripped
     */
    private CountDownLatch schedule( Scheduler rxScheduler, final int totalCount, final Semaphore semaphore ) {

        final CountDownLatch latch = new CountDownLatch( totalCount );

        for ( int i = 0; i < totalCount; i++ ) {
            final Action0 action = new Action0() {
                @Override
                public void call() {
                    try {
                        final String threadName = Thread.currentThread().getName();

                        LOG.info( "{} trying to acquire semaphore", threadName );
                        //get and release the lock
                        semaphore.acquire();

                        LOG.info( "{} has acquired sempahore", threadName );

                        //countdown the latch
                        latch.countDown();
                    }
                    catch ( InterruptedException e ) {
                        throw new RuntimeException( e );
                    }
                }
            };
            rxScheduler.schedule( action );
        }


        return latch;
    }
}



