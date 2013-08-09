package com.usergrid.count;

import com.usergrid.count.common.Count;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

/**
 * @author zznate
 */
public class BatchCountParallelismTest {

  private ExecutorService exec = Executors.newFixedThreadPool(24);
  private SimpleBatcher batcher;
  private StubSubmitter submitter = new StubSubmitter();

  private AtomicLong submits;

  @Before
  public void setupLocal() {
    submits = new AtomicLong();

    batcher = new SimpleBatcher();
    batcher.setBatchSize(10);
    batcher.setBatchSubmitter(submitter);

  }

  @Test
  public void verifyConcurrentAdd() throws Exception {
    List<Future<Boolean>> calls = new ArrayList<Future<Boolean>>();
    // create 10 tasks
    // submit should be invoked 10 times
    final CountDownLatch cdl = new CountDownLatch(10);
    for (int x=0; x<10; x++){
      final int c = x;

      // each task should increment the counter 10 times

      calls.add(exec.submit(new Callable<Boolean>() {

        @Override
        public Boolean call() throws Exception {
          // should increment this counter to 10 for this thread, 100 overall
          // this is invoked 10 times
          for (int y=0; y<10; y++) {
            Count count = new Count("Counter","k1","counter1", 1);
            batcher.add(count);
          }
          System.out.println("c: " + c);
          cdl.countDown();
          return new Boolean(true);

        }
      }));

    }
    batcher.add(new Count("Counter","k1","counter1",1));
    System.out.println("size: " +calls.size());

    cdl.await();
    exec.awaitTermination(2,TimeUnit.SECONDS);

    //exec.awaitTermination(3, TimeUnit.SECONDS);
    // we should have 100 total invocations of AbstractBatcher#add
    assertEquals(101, batcher.invocationCounter.count());
    // we should have submitted 10 batches

    assertEquals(10, batcher.getBatchSubmissionCount());

    // the first batch should have a size 10 TODO currently 11 though :(


  }

  class StubSubmitter implements BatchSubmitter {

    AtomicLong counted = new AtomicLong();
    AtomicLong submit = new AtomicLong();
    @Override
    public Future<?> submit(Collection<Count> counts) {
      System.out.println("submitted: " + counts.size());
      counted.addAndGet(counts.size());
      submit.incrementAndGet();
      return null;
    }

    @Override
    public void shutdown() {
      //To change body of implemented methods use File | Settings | File Templates.
    }
  }

}
