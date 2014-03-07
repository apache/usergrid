package org.apache.usergrid.persistence.collection.rx;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;

import com.google.inject.Inject;

import rx.Scheduler;
import rx.functions.Action1;
import rx.util.functions.Action0;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Test for our scheduler
 */
@RunWith( JukitoRunner.class )
@UseModules( TestCollectionModule.class )
public class CassandraThreadSchedulerTest {


    private static final Logger LOG = LoggerFactory.getLogger( CassandraThreadSchedulerTest.class );

    /**
     * Number of milliseconds to wait when trying to acquire the semaphore
     */
    private static final long TEST_TIMEOUT = 30000;

    @Inject
    private RxFig rxFig;


    @Test
    public void testMaxLimit() throws InterruptedException {

        final int maxCount = 10;

        rxFig.override( RxFig.PROP_THREAD, ""+ maxCount );

        final CassandraThreadScheduler cassSchedulerSetup = new CassandraThreadScheduler( rxFig );

        final Scheduler rxScheduler = cassSchedulerSetup.get();

        //we want a fair semaphore so we can release in acquire order
        final Semaphore semaphore = new Semaphore( 0, true );

        //we should not have maxCount actions running in the scheduler
        CountDownLatch result = schedule( rxScheduler, rxFig.getMaxThreadCount(), semaphore, TEST_TIMEOUT );

        //schedule and we should fail

        try {
            rxScheduler.schedule( new Action1<Scheduler.Inner>() {
                            @Override
                            public void call( final Scheduler.Inner inner ) {
                            }
                            });

            fail( "This should have thrown an exception" );
        }
        catch ( RejectedExecutionException ree ) {
            //swallow, we just want to ensure we catch this to continue the test
        }

        //now release the semaphore so all 10 can run
        semaphore.release( rxFig.getMaxThreadCount() );

        //wait for completion
        boolean completed = result.await( 20, TimeUnit.SECONDS );

        assertTrue( "Completed executing actions", completed );

        //verify we can schedule and execute a new operation
        result = schedule( rxScheduler, 1, semaphore, TEST_TIMEOUT );

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
        rxFig.override( RxFig.PROP_THREAD, "" + maxCount );

        final CassandraThreadScheduler cassSchedulerSetup = new CassandraThreadScheduler( rxFig );

        final Scheduler rxScheduler = cassSchedulerSetup.get();

        //we want a fair semaphore so we can release in acquire order
        final Semaphore semaphore = new Semaphore( 0, true );

        //we should not have maxCount actions running in the scheduler
        CountDownLatch firstHalf = schedule( rxScheduler, half, semaphore, TEST_TIMEOUT );

        //schedule the second half
        CountDownLatch secondHalf = schedule( rxScheduler, half, semaphore, TEST_TIMEOUT );

        //schedule and we should fail

        try {

            rxScheduler.schedule(  new Action1<Scheduler.Inner>() {
                            @Override
                            public void call( final Scheduler.Inner inner ) {
                    //no op
                }
            } );

            fail( "This should have thrown an exception" );
        }
        catch ( RejectedExecutionException ree ) {
            //swallow, we just want to ensure we catch this to continue the test
        }


        //update the property to shrink the size
        rxFig.override( RxFig.PROP_THREAD, "" + half );

        //now release the first half of executors
        semaphore.release( half );


        //wait for completion
        boolean completed = firstHalf.await( 20, TimeUnit.SECONDS );

        assertTrue( "Completed executing actions", completed );

        //verify we can't schedule b/c we're still at capacity


        try {

            rxScheduler.schedule(  new Action1<Scheduler.Inner>() {
                            @Override
                            public void call( final Scheduler.Inner inner ) {
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
        CountDownLatch newJob = schedule( rxScheduler, 1, semaphore, TEST_TIMEOUT  );

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
        rxFig.override( RxFig.PROP_THREAD, "" + startCount );

        final CassandraThreadScheduler cassSchedulerSetup = new CassandraThreadScheduler( rxFig );

        final Scheduler rxScheduler = cassSchedulerSetup.get();

        //we want a fair semaphore so we can release in acquire order
        final Semaphore semaphore = new Semaphore( 0, true );

        //we should not have maxCount actions running in the scheduler
        CountDownLatch firstBatch = schedule( rxScheduler, rxFig.getMaxThreadCount(), semaphore, TEST_TIMEOUT  );

        //schedule and we should fail

        try {

            rxScheduler.schedule(  new Action1<Scheduler.Inner>() {
                            @Override
                            public void call( final Scheduler.Inner inner ) {
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
        rxFig.override( RxFig.PROP_THREAD, "" + doubleMaxCount );


        //now schedule 10 more

        CountDownLatch secondBatch = schedule( rxScheduler, rxFig.getMaxThreadCount() - startCount, semaphore, TEST_TIMEOUT  );

        //this should fail.  We're at capacity

        try {

            rxScheduler.schedule(  new Action1<Scheduler.Inner>() {
                            @Override
                            public void call( final Scheduler.Inner inner ) {
                    //no op
                }
            } );

            fail( "This should have thrown an exception" );
        }
        catch ( RejectedExecutionException ree ) {
            //swallow, we just want to ensure we catch this to continue the test
        }


        //now release the semaphores so all
        semaphore.release( rxFig.getMaxThreadCount() );

        //wait for completion
        boolean completed = firstBatch.await( 20, TimeUnit.SECONDS );

        assertTrue( "Completed executing actions", completed );

        completed = secondBatch.await( 20, TimeUnit.SECONDS );

        assertTrue( "Completed executing actions", completed );

        //verify we can schedule and execute a new operation
        CountDownLatch result = schedule( rxScheduler, 1, semaphore, TEST_TIMEOUT  );

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
    private CountDownLatch schedule( Scheduler rxScheduler, final int totalCount, final Semaphore semaphore, final long timeout ) {

        final CountDownLatch latch = new CountDownLatch( totalCount );

        for ( int i = 0; i < totalCount; i++ ) {

            final Action1<Scheduler.Inner> action = new  Action1<Scheduler.Inner>() {
                                       @Override
                                       public void call( final Scheduler.Inner inner ) {
                    try {
                        final String threadName = Thread.currentThread().getName();

                        LOG.info( "{} trying to acquire semaphore", threadName );
                        //get and release the lock
                        semaphore.tryAcquire( timeout, TimeUnit.MILLISECONDS );

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



