package org.usergrid.cassandra;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(CassandraRunner.class)
public class CassandraRunnerTest {

    private Logger logger = LoggerFactory.getLogger(CassandraRunnerTest.class);
    //@Autowired
    // This works - must be with annotation scanning
    String testBean = CassandraRunner.getBean("testBean", String.class);
    //@Resource
    //private String testBean;

    @Test
    public void simpleTest() {
        //testBean = CassandraRunner.getAc().getBean("testBean", String.class);
        logger.info("info");
        assertTrue(true);
        assertEquals("testValue",testBean);
    }
}
