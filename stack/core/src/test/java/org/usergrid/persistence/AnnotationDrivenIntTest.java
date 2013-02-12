package org.usergrid.persistence;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.usergrid.cassandra.CassandraRunner;
import org.usergrid.cassandra.DataControl;

import static org.junit.Assert.assertTrue;

/**
 * @author zznate
 */
@RunWith(CassandraRunner.class)
@DataControl(schemaManager = "coreManager")
public class AnnotationDrivenIntTest {

    @Test
    public void shouldSpinUp() throws Exception {
        assertTrue(true);
    }
}
