package org.usergrid.cassandra;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(CassandraRunner.class)
public class CassandraRunnerTest {

    //@Autowired
    // This works - must be with annotation scanning
    String testBean = CassandraRunner.getBean("testBean", String.class);

    @Test
    public void simpleTest() {
        //testBean = CassandraRunner.getAc().getBean("testBean", String.class);
        assertTrue(true);
        assertEquals("testValue",testBean);
    }
}
