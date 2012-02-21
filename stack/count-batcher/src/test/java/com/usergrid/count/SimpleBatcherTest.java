package com.usergrid.count;

import com.usergrid.count.common.Count;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Unit test for simple SimpleBatcher.
 */
public class SimpleBatcherTest {

    @Before
    public void setupLocal() {

    }


    @Test
    public void testBatchSizeTrigger() {
        SimpleBatcher simpleBatcher = new SimpleBatcher(1);
        simpleBatcher.setBatchSubmitter(new Slf4JBatchSubmitter());
        simpleBatcher.setBatchSize(4);
        simpleBatcher.add(new Count("Counter","k1","counter1", 1));
        simpleBatcher.add(new Count("Counter","k1","c2", 2));
        simpleBatcher.add(new Count("Counter","k1","c3",1));
        simpleBatcher.add(new Count("Counter","k1","c3", 1));
        simpleBatcher.add(new Count("Counter","k1","c3",1));

        assertEquals(1, simpleBatcher.getBatchSubmissionCount());
        simpleBatcher.add(new Count("Counter","k1","c4",1));
        assertEquals(1, simpleBatcher.getBatchSubmissionCount());

    }

}
