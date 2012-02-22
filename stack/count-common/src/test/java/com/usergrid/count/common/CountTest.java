package com.usergrid.count.common;

import me.prettyprint.cassandra.service.clock.MicrosecondsClockResolution;
import me.prettyprint.cassandra.utils.TimeUUIDUtils;
import me.prettyprint.hector.api.factory.HFactory;
import org.junit.Test;

import java.util.UUID;

import static junit.framework.Assert.assertEquals;

/**
 * Unit test for count object machinations
 */
public class CountTest {

    @Test
    public void testCounterName() {
        Count count = new Count("Counters","k1","c1",1);
        assertEquals("Counters:k1:c1",count.getCounterName());
    }

    @Test
    public void testApplyCount() {
        Count count = new Count("Counters","k1","c1",1);
        Count c2 = new Count("Counters","k1","c1",1);
        Count c3 = new Count("Counters","k1","c1",1);
        count.apply(c2).apply(c3);
        assertEquals(3, count.getValue());
    }

    @Test
    public void testApplyCountMixedTypes() {
        Count count = new Count("Counters",1,3,1);
        Count c2 = new Count("Counters",1,3,1);
        Count c3 = new Count("Counters",1,3,1);
        count.apply(c3).apply(c2);
        assertEquals(3,count.getValue());
    }


    @Test(expected = IllegalArgumentException.class )
    public void testApplyFail_onKeyname() {
        Count count = new Count("Counters","k1","c1",1);
        Count c2 = new Count("Coutenrs","k2","c1",1);
        count.apply(c2);
    }

    @Test(expected = IllegalArgumentException.class )
    public void testApplyFail_onColumnname() {
        Count count = new Count("Counters","k1","c1",1);
        Count c2 = new Count("Counters","k1","c2",1);
        count.apply(c2);
    }

}
