package org.usergrid.cassandra;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

@RunWith(CassandraRunner.class)
public class CassandraRunnerTest {

    @Test
    public void simpleTest() {
        assertTrue(true);
    }
}
