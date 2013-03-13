package org.usergrid.persistence;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.cassandra.CassandraRunner;
import org.usergrid.cassandra.DataControl;

import static org.junit.Assert.assertTrue;

/**
 * @author zznate
 */
@RunWith(CassandraRunner.class)
@DataControl(schemaManager = "coreManager")
public class AnnotationDrivenIntTest {
    private Logger logger = LoggerFactory.getLogger(AnnotationDrivenIntTest.class);

    @Test
    public void shouldSpinUp() throws Exception {
        logger.info("shouldSpinUp");
        assertTrue(true);
    }

    @Test
    public void shouldStillBeUp() throws Exception {
        logger.info("shouldStillBeUp");
        assertTrue(true);
    }

}
