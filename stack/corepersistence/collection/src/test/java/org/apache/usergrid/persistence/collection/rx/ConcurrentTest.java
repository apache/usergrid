package org.apache.usergrid.persistence.collection.rx;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import rx.Observable;
import rx.concurrency.Schedulers;
import rx.util.functions.Func1;

import static org.junit.Assert.assertEquals;


/**
 *
 *
 */
public class ConcurrentTest {

    private static final Logger logger = LoggerFactory.getLogger(ConcurrentTest.class);

    @Test
    public void concurrent(){

        final String source = "test";
        Observable<String> observable = Observable.from(source);

        //we could theoretically use the same instance 3 times.  I just want to use
        //3 actual instances, since this is closer to the real use case.

        final CountDownLatch latch = new CountDownLatch( 0 );
        TestConcurrent instance1 = new TestConcurrent( latch );
        TestConcurrent instance2 = new TestConcurrent( latch );
        TestConcurrent instance3 = new TestConcurrent( latch );

        //concurrent inherits thread pool from it's observable, set it's thread pool

        observable = observable.subscribeOn( Schedulers.threadPoolForIO() );

        Observable<String> result = Concurrent
                .concurrent(observable, instance1, instance2, instance3 );

        assertEquals( "No invocation yet", 0, set.size() );


        //now invoke it
        String response = result.toBlockingObservable().single();


        assertEquals( "Same value emitted", source, response );

        //verify each function executed in it's own thread

        assertEquals( "3 thread invoked", 3, set.size() );

        //print them out just for giggles
        for(Multiset.Entry<String> entry: set.entrySet()){
            System.out.println( entry.getElement() );
            assertEquals( "1 Thread per invocation", 1, entry.getCount() );
        }



    }


    private Multiset<String> set  = HashMultiset.create();


    /**
     * Simple function that just adds data to our multiset for later evaluation
     */
    public class TestConcurrent implements Func1<String, String>{

        private final  CountDownLatch latch;


        public TestConcurrent( final  CountDownLatch latch) {
            this.latch = latch;}


        @Override
        public String call( final String s ) {
            final String threadName = Thread.currentThread().getName();

            logger.info("Function executing on thread {}", threadName);

            set.add( threadName );

            //we want to make sure each thread blocks until they all have passed the latch
            //this way we can ensure they're all running concurrently
            try {
               latch.countDown();
               latch.await( 30, TimeUnit.SECONDS );

            }
            catch ( InterruptedException e ) {
                logger.error( "Runner interrupted", e );

            }

            return s;
        }
    }
}
