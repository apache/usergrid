package org.usergrid.system;

import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.testutils.EmbeddedServerHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author zznate
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="/testApplicationContext.xml")
public class UsergridSystemMonitorTest {
  private static EmbeddedServerHelper embedded;

  @Resource
  private UsergridSystemMonitor usergridSystemMonitor;

  @Test
  public void testVersionNumber() {
    assertEquals("0.1", usergridSystemMonitor.getBuildNumber());
  }

  @Test
  public void testIsCassandraAlive() {
    assertTrue(usergridSystemMonitor.getIsCassandraAlive());
  }

  @BeforeClass
  public static void setup() throws Exception {
    embedded = new EmbeddedServerHelper();
    embedded.setup();
  }

  @AfterClass
  public static void teardown() {
    EmbeddedServerHelper.teardown();
  }

}
