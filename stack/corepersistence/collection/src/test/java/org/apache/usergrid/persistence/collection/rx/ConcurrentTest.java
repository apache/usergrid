package org.apache.usergrid.persistence.collection.rx;



import java.util.concurrent.ExecutorService;

import org.antlr.misc.MultiMap;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import rx.Observable;
import rx.schedulers.Schedulers;
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
        TestConcurrent instance1 = new TestConcurrent();
        TestConcurrent instance2 = new TestConcurrent();
        TestConcurrent instance3 = new TestConcurrent();

        Observable<String> result = Concurrent.concurrent( Schedulers.threadPoolForIO(), observable, instance1, instance2, instance3 );

        assertEquals("No invocation yet", 0, set.size());


        //now invoke it
        String response = result.toBlockingObservable().single();

        assertEquals("Same value emitted",source, response );

        //verify each function executed in it's own thread

        assertEquals("3 thread invoked", 3, set.size());

        //print them out just for giggles
        for(Multiset.Entry<String> entry: set.entrySet()){
            System.out.println( entry.getElement() );
            assertEquals("1 Thread per invocation", 1, entry.getCount());
        }

    }


    private Multiset<String> set  = HashMultiset.create();


    /**
     * Simple function that just adds data to our multiset for later evaluation
     */
    public class TestConcurrent implements Func1<String, String>{

        @Override
        public String call( final String s ) {
            final String threadName = Thread.currentThread().getName();

            logger.info("Function executing on thread {}", threadName);

            set.add( threadName );

            //we sleep to ensure we don't run so fast that a thread is reused in our test
            try {
                Thread.sleep(1000);
            }
            catch ( InterruptedException e ) {
                logger.error( "Runner interrupted", e );

            }

            return s;
        }
    }
}
