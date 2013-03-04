package org.usergrid.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.annotation.Resource;

import me.prettyprint.hector.testutils.EmbeddedServerHelper;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.usergrid.cassandra.CassandraRunner;

/**
 * @author zznate
 */
@RunWith(CassandraRunner.class)
public class UsergridSystemMonitorTest {


    private UsergridSystemMonitor usergridSystemMonitor;

    @Before
    public void setupLocal() {
        usergridSystemMonitor = CassandraRunner.getBean(UsergridSystemMonitor.class);
    }

    @Test
    public void testVersionNumber() {
        assertEquals("0.1", usergridSystemMonitor.getBuildNumber());
    }

    @Test
    public void testIsCassandraAlive() {
        assertTrue(usergridSystemMonitor.getIsCassandraAlive());
    }


}
