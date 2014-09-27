package org.apache.usergrid.persistence.core.task;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;


/**
 * Tests for the namedtask execution impl
 */
public class NamedTaskExecutorImplTest {


    @Test
    public void jobSuccess() throws InterruptedException {
        final TaskExecutor executor = new NamedTaskExecutorImpl( "jobSuccess", 1 );

        final CountDownLatch exceptionLatch = new CountDownLatch( 0 );
        final CountDownLatch rejectedLatch = new CountDownLatch( 0 );
        final CountDownLatch runLatch = new CountDownLatch( 1 );

        final Task<Void> task = new TestTask<Void>( exceptionLatch, rejectedLatch, runLatch ) {};

        executor.submit( task );


        runLatch.await( 1000, TimeUnit.MILLISECONDS );

        assertEquals( 0l, exceptionLatch.getCount() );

        assertEquals( 0l, rejectedLatch.getCount() );
    }


    @Test
    public void exceptionThrown() throws InterruptedException {
        final TaskExecutor executor = new NamedTaskExecutorImpl( "jobSuccess", 1 );

        final CountDownLatch exceptionLatch = new CountDownLatch( 1 );
        final CountDownLatch rejectedLatch = new CountDownLatch( 0 );
        final CountDownLatch runLatch = new CountDownLatch( 1 );

        final RuntimeException re = new RuntimeException( "throwing exception" );

        final TestTask<Void> task = new TestTask<Void>( exceptionLatch, rejectedLatch, runLatch ) {


            @Override
            public Void executeTask() {
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
        final TaskExecutor executor = new NamedTaskExecutorImpl( "jobSuccess", 1 );

        final CountDownLatch exceptionLatch = new CountDownLatch( 0 );
        final CountDownLatch rejectedLatch = new CountDownLatch( 0 );
        final CountDownLatch runLatch = new CountDownLatch( 1 );


        final TestTask<Void> task = new TestTask<Void>( exceptionLatch, rejectedLatch, runLatch ) {
            @Override
            public Void executeTask() throws Exception {
                super.executeTask();

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
       

        final TaskExecutor executor = new NamedTaskExecutorImpl( "jobSuccess", threadPoolSize );

        final CountDownLatch exceptionLatch = new CountDownLatch( 0 );
        final CountDownLatch rejectedLatch = new CountDownLatch( 0 );
        final CountDownLatch runLatch = new CountDownLatch( 1 );

        int iterations = threadPoolSize ;

        for ( int i = 0; i < iterations; i++ ) {

            final TestTask<Void> task = new TestTask<Void>( exceptionLatch, rejectedLatch, runLatch ) {
                @Override
                public Void executeTask() throws Exception {
                    super.executeTask();

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


    @Test
    public void jobTreeResult() throws InterruptedException {

        final int threadPoolSize = 4;
       

        final TaskExecutor executor = new NamedTaskExecutorImpl( "jobSuccess", threadPoolSize );

        final CountDownLatch exceptionLatch = new CountDownLatch( 0 );
        final CountDownLatch rejectedLatch = new CountDownLatch( 0 );

        //accomodates for splitting the job 1->2->4 and joining
        final CountDownLatch runLatch = new CountDownLatch( 7 );


        TestRecursiveTask task = new TestRecursiveTask( exceptionLatch, rejectedLatch, runLatch, 1, 3 );

         executor.submit( task );


        //compute our result
        Integer result = task.join();

        //result should be 1+2*2+3*4
        final int expected = 4*3;

        assertEquals(expected, result.intValue());

        //just to check our latches
        runLatch.await( 1000, TimeUnit.MILLISECONDS );

        //now submit the second task


    }


    private static class TestRecursiveTask extends TestTask<Integer> {

        private final int depth;
        private final int maxDepth;

        private TestRecursiveTask( final CountDownLatch exceptionLatch, final CountDownLatch rejectedLatch,
                                   final CountDownLatch runLatch, final int depth, final int maxDepth ) {
            super( exceptionLatch, rejectedLatch, runLatch );
            this.depth = depth;
            this.maxDepth = maxDepth;
        }


        @Override
        public Integer executeTask() throws Exception {

            if(depth == maxDepth ){
                return depth;
            }

            TestRecursiveTask left = new TestRecursiveTask(exceptionLatch, rejectedLatch, runLatch, depth+1, maxDepth  );

            TestRecursiveTask right = new TestRecursiveTask(exceptionLatch, rejectedLatch, runLatch, depth+1, maxDepth  );

            //run our left in another thread
            left.fork();

            return right.compute() + left.join();
        }
    }


    private static abstract class TestTask<V> extends Task<V> {

        protected final List<Throwable> exceptions;
        protected final CountDownLatch exceptionLatch;
        protected final CountDownLatch rejectedLatch;
        protected final CountDownLatch runLatch;


        private TestTask( final CountDownLatch exceptionLatch, final CountDownLatch rejectedLatch,
                          final CountDownLatch runLatch ) {
            this.exceptionLatch = exceptionLatch;
            this.rejectedLatch = rejectedLatch;
            this.runLatch = runLatch;

            this.exceptions = new ArrayList<>();
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
        public V executeTask() throws Exception {
            runLatch.countDown();
            return null;
        }
    }
}
