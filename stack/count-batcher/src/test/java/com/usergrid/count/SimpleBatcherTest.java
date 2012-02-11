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
    public void testAddCountForPayloadSize() {
        SimpleBatcher simpleBatcher = new SimpleBatcher();
        simpleBatcher.add(new Count("k1","counter1",1));
        simpleBatcher.add(new Count("k1","counter1",5));
        assertEquals(1, simpleBatcher.getPayloadSize());
    }

    @Test
    public void testBatchSizeTrigger() {
        SimpleBatcher simpleBatcher = new SimpleBatcher();
        simpleBatcher.setBatchSubmitter(new Slf4JBatchSubmitter());
        simpleBatcher.setBatchSize(4);
        simpleBatcher.add(new Count("k1","counter1", 1));
        simpleBatcher.add(new Count("k1","c2", 2));
        simpleBatcher.add(new Count("k1","c3",1));
        simpleBatcher.add(new Count("k1","c3", 1));
        simpleBatcher.add(new Count("k1","c3",1));

        assertEquals(3, simpleBatcher.getPayloadSize());
        simpleBatcher.add(new Count("k1","c4",1));
        assertEquals(0, simpleBatcher.getPayloadSize());
        simpleBatcher.add(new Count("k1","c3",1));
        assertEquals(1, simpleBatcher.getPayloadSize());
    }

}
