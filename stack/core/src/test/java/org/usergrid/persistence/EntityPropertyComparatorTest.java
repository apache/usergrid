package org.usergrid.persistence;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test for the entity comparator
 *
 * @author: tnine
 *
 */
public class EntityPropertyComparatorTest {


  @Test
  public void testNulls() throws Exception {

    DynamicEntity first = new DynamicEntity();
    first.setProperty("test", true);

    EntityPropertyComparator forward = new EntityPropertyComparator("test", false);


    assertEquals(0, forward.compare(null, null));

    assertEquals(-1, forward.compare(first, null));

    assertEquals(1, forward.compare(null, first));


    //now test in reverse

    EntityPropertyComparator reverse = new EntityPropertyComparator("test", true);


    assertEquals(0, reverse.compare(null, null));

    assertEquals(-1, reverse.compare(first, null));

    assertEquals(1, reverse.compare(null, first));

  }

  @Test
  public void testBooleans() throws Exception {

    DynamicEntity second = new DynamicEntity();
    second.setProperty("test", true);


    DynamicEntity first = new DynamicEntity();
    first.setProperty("test", false);

    EntityPropertyComparator forward = new EntityPropertyComparator("test", false);


    assertEquals(0, forward.compare(second, second));

    assertEquals(1, forward.compare(second, first));

    assertEquals(-1, forward.compare(first, second));


    //now test in reverse

    EntityPropertyComparator reverse = new EntityPropertyComparator("test", true);


    assertEquals(0, reverse.compare(second, second));

    assertEquals(1, reverse.compare(first, second));

    assertEquals(-1, reverse.compare(second, first));

  }


  @Test
  public void testFloat() throws Exception {

    DynamicEntity second = new DynamicEntity();
    second.setProperty("test", 1.0f);


    DynamicEntity first = new DynamicEntity();
    first.setProperty("test", 0.0f);

    EntityPropertyComparator forward = new EntityPropertyComparator("test", false);


    assertEquals(0, forward.compare(second, second));

    assertEquals(1, forward.compare(second, first));

    assertEquals(-1, forward.compare(first, second));


    //now test in reverse

    EntityPropertyComparator reverse = new EntityPropertyComparator("test", true);


    assertEquals(0, reverse.compare(second, second));

    assertEquals(1, reverse.compare(first, second));

    assertEquals(-1, reverse.compare(second, first));

  }


  @Test
  public void testLong() throws Exception {

    DynamicEntity second = new DynamicEntity();
    second.setProperty("test", 1l);


    DynamicEntity first = new DynamicEntity();
    first.setProperty("test", 0l);

    EntityPropertyComparator forward = new EntityPropertyComparator("test", false);


    assertEquals(0, forward.compare(second, second));

    assertEquals(1, forward.compare(second, first));

    assertEquals(-1, forward.compare(first, second));


    //now test in reverse

    EntityPropertyComparator reverse = new EntityPropertyComparator("test", true);


    assertEquals(0, reverse.compare(second, second));

    assertEquals(1, reverse.compare(first, second));

    assertEquals(-1, reverse.compare(second, first));

  }


  @Test
  public void testDouble() throws Exception {

    DynamicEntity second = new DynamicEntity();
    second.setProperty("test", 1d);


    DynamicEntity first = new DynamicEntity();
    first.setProperty("test", 0d);


    EntityPropertyComparator forward = new EntityPropertyComparator("test", false);


    assertEquals(0, forward.compare(second, second));

    assertEquals(1, forward.compare(second, first));

    assertEquals(-1, forward.compare(first, second));


    //now test in reverse

    EntityPropertyComparator reverse = new EntityPropertyComparator("test", true);


    assertEquals(0, reverse.compare(second, second));

    assertEquals(1, reverse.compare(first, second));

    assertEquals(-1, reverse.compare(second, first));


  }


  @Test
  public void testString() throws Exception {

    DynamicEntity second = new DynamicEntity();
    second.setProperty("test", "b");


    DynamicEntity first = new DynamicEntity();
    first.setProperty("test", "a");

    EntityPropertyComparator forward = new EntityPropertyComparator("test", false);


    assertEquals(0, forward.compare(second, second));

    assertEquals(1, forward.compare(second, first));

    assertEquals(-1, forward.compare(first, second));


    //now test in reverse

    EntityPropertyComparator reverse = new EntityPropertyComparator("test", true);


    assertEquals(0, reverse.compare(second, second));

    assertEquals(1, reverse.compare(first, second));

    assertEquals(-1, reverse.compare(second, first));

  }
}
