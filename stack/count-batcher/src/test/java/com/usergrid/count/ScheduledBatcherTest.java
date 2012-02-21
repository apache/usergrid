package com.usergrid.count;

import com.usergrid.count.common.Count;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;

/**
 * @author zznate
 */
public class ScheduledBatcherTest {

    @Test
    public void testScheduledExecution() {
        ScheduledBatcher batcher = new ScheduledBatcher(1,1);
        batcher.setBatchSubmitter(new Slf4JBatchSubmitter());
        batcher.add(new Count("Counter","k1","c1",1));
        batcher.add(new Count("Counter","k1","c1",3));
        batcher.add(new Count("Counter","k1","c2",1));
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        batcher.add(new Count("Counter","k1","c2",1));
        assertEquals(1,batcher.getBatchSubmissionCount());

    }
}
