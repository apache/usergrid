package org.apache.usergrid.persistence.core.task;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import junit.framework.TestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;


/**
 * Tests for the namedtask execution impl
 */
public class NamedTaskExecutorImplTest {


    @Test
    public void jobSuccess() throws InterruptedException {
        final TaskExecutor executor = new NamedTaskExecutorImpl( "jobSuccess", 1, 0 );

        final CountDownLatch exceptionLatch = new CountDownLatch( 0 );
        final CountDownLatch rejectedLatch = new CountDownLatch( 0 );
        final CountDownLatch runLatch = new CountDownLatch( 1 );

        final Task<Void, UUID> task = new TestTask<Void>( exceptionLatch, rejectedLatch, runLatch ) {};

        executor.submit( task );


        runLatch.await( 1000, TimeUnit.MILLISECONDS );

        assertEquals( 0l, exceptionLatch.getCount() );

        assertEquals( 0l, rejectedLatch.getCount() );
    }


    @Test
    public void exceptionThrown() throws InterruptedException {
        final TaskExecutor executor = new NamedTaskExecutorImpl( "jobSuccess", 1, 0 );

        final CountDownLatch exceptionLatch = new CountDownLatch( 1 );
        final CountDownLatch rejectedLatch = new CountDownLatch( 0 );
        final CountDownLatch runLatch = new CountDownLatch( 1 );

        final RuntimeException re = new RuntimeException( "throwing exception" );

        final TestTask<Void> task = new TestTask<Void>( exceptionLatch, rejectedLatch, runLatch ) {
            @Override
            public Void call() throws Exception {
                super.call();
                throw re;
            }
        };

        executor.submit( task );


        runLatch.await( 1000, TimeUnit.MILLISECONDS );
        exceptionLatch.await( 1000, TimeUnit.MILLISECONDS );

        assertSame( re, task.exceptions.get( 0 ) );


        assertEquals( 0l, rejectedLatch.getCount() );
    }


    @Test
    public void noCapacity() throws InterruptedException {
        final TaskExecutor executor = new NamedTaskExecutorImpl( "jobSuccess", 1, 0 );

        final CountDownLatch exceptionLatch = new CountDownLatch( 0 );
        final CountDownLatch rejectedLatch = new CountDownLatch( 0 );
        final CountDownLatch runLatch = new CountDownLatch( 1 );


        final TestTask<Void> task = new TestTask<Void>( exceptionLatch, rejectedLatch, runLatch ) {
            @Override
            public Void call() throws Exception {
                super.call();

                //park this thread so it takes up a task and the next is rejected
                final Object mutex = new Object();

                synchronized ( mutex ) {
                    mutex.wait();
                }

                return null;
            }
        };

        executor.submit( task );


        runLatch.await( 1000, TimeUnit.MILLISECONDS );

        //now submit the second task


        final CountDownLatch secondRejectedLatch = new CountDownLatch( 1 );
        final CountDownLatch secondExceptionLatch = new CountDownLatch( 0 );
        final CountDownLatch secondRunLatch = new CountDownLatch( 1 );


        final TestTask<Void> testTask = new TestTask<Void>( exceptionLatch, rejectedLatch, runLatch ) {};

        executor.submit( testTask );


        secondRejectedLatch.await( 1000, TimeUnit.MILLISECONDS );

        //if we get here we've been rejected, just double check we didn't run

        assertEquals( 1l, secondRunLatch.getCount() );
        assertEquals( 0l, secondExceptionLatch.getCount() );
    }


    @Test
    public void noCapacityWithQueue() throws InterruptedException {

        final int threadPoolSize = 1;
        final int queueSize = 10;

        final TaskExecutor executor = new NamedTaskExecutorImpl( "jobSuccess", threadPoolSize, queueSize );

        final CountDownLatch exceptionLatch = new CountDownLatch( 0 );
        final CountDownLatch rejectedLatch = new CountDownLatch( 0 );
        final CountDownLatch runLatch = new CountDownLatch( 1 );

        int iterations = threadPoolSize + queueSize;

        for(int i = 0; i < iterations; i ++) {

            final TestTask<Void> task = new TestTask<Void>( exceptionLatch, rejectedLatch, runLatch ) {
                @Override
                public Void call() throws Exception {
                    super.call();

                    //park this thread so it takes up a task and the next is rejected
                    final Object mutex = new Object();

                    synchronized ( mutex ) {
                        mutex.wait();
                    }

                    return null;
                }
            };
            executor.submit( task );
        }



        runLatch.await( 1000, TimeUnit.MILLISECONDS );

        //now submit the second task


        final CountDownLatch secondRejectedLatch = new CountDownLatch( 1 );
        final CountDownLatch secondExceptionLatch = new CountDownLatch( 0 );
        final CountDownLatch secondRunLatch = new CountDownLatch( 1 );


        final TestTask<Void> testTask = new TestTask<Void>( exceptionLatch, rejectedLatch, runLatch ) {};

        executor.submit( testTask );


        secondRejectedLatch.await( 1000, TimeUnit.MILLISECONDS );

        //if we get here we've been rejected, just double check we didn't run

        assertEquals( 1l, secondRunLatch.getCount() );
        assertEquals( 0l, secondExceptionLatch.getCount() );
    }


    private static abstract class TestTask<V> implements Task<V, UUID> {

        private final List<Throwable> exceptions;
        private final CountDownLatch exceptionLatch;
        private final CountDownLatch rejectedLatch;
        private final CountDownLatch runLatch;


        private TestTask( final CountDownLatch exceptionLatch, final CountDownLatch rejectedLatch,
                          final CountDownLatch runLatch ) {
            this.exceptionLatch = exceptionLatch;
            this.rejectedLatch = rejectedLatch;
            this.runLatch = runLatch;

            this.exceptions = new ArrayList<>();
        }


        @Override
        public UUID getId() {
            return UUIDGenerator.newTimeUUID();
        }


        @Override
        public void exceptionThrown( final Throwable throwable ) {
            this.exceptions.add( throwable );
            exceptionLatch.countDown();
        }


        @Override
        public void rejected() {
            rejectedLatch.countDown();
        }


        @Override
        public V call() throws Exception {
            runLatch.countDown();
            return null;
        }
    }
}
