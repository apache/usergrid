package org.usergrid.utils;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * @author zznate
 */
public class TimeUtilsTest {

  @Test
  public void fromSingleValues() {
    assertEquals(172800000L, TimeUtils.millisFromDuration("2d"));
    assertEquals(420000L, TimeUtils.millisFromDuration("7m"));
    assertEquals(90000L, TimeUtils.millisFromDuration("90s"));
    assertEquals(TimeUtils.millisFromDuration("1d"),TimeUtils.millisFromDuration("24h"));
    assertEquals(250L, TimeUtils.millisFromDuration("250S"));
  }

  @Test
  public void compoundValues() {
    assertEquals(65000L, TimeUtils.millisFromDuration("1m,5s"));
    assertEquals(1293484000L, TimeUtils.millisFromDuration("14d,23h,18m,4s"));
    assertEquals(1293484000L, TimeUtils.millisFromDuration("18m,23h,4s,14d"));
    assertEquals(8005L, TimeUtils.millisFromDuration("8s,5S"));
    assertEquals(16137L, TimeUtils.millisFromDuration("15s,1137"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void meaningfulFailure() {
    TimeUtils.millisFromDuration("14z");
  }

  @Test
  public void passThrough() {
    assertEquals(5508L, TimeUtils.millisFromDuration("5508"));
  }
}


