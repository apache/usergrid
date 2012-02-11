package com.usergrid.count.common;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Unit test for count object machinations
 */
public class CountTest {

    @Test
    public void testCounterName() {
        Count count = new Count("k1","c1",1);
        assertEquals("k1:c1",count.getCounterName());
    }

    @Test
    public void testApplyCount() {
        Count count = new Count("k1","c1",1);
        Count c2 = new Count("k1","c1",1);
        Count c3 = new Count("k1","c1",1);
        count.apply(c2).apply(c3);
        assertEquals(3, count.getValue());
    }

    @Test(expected = IllegalArgumentException.class )
    public void testApplyFail_onKeyname() {
        Count count = new Count("k1","c1",1);
        Count c2 = new Count("k2","c1",1);
        count.apply(c2);
    }

    @Test(expected = IllegalArgumentException.class )
    public void testApplyFail_onColumnname() {
        Count count = new Count("k1","c1",1);
        Count c2 = new Count("k1","c2",1);
        count.apply(c2);
    }

}
