package com.usergrid.count;

import com.usergrid.count.common.Count;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.testutils.EmbeddedServerHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

/**
 * @author zznate
 */
public class CassandraCounterStoreTest {

    private static EmbeddedServerHelper esh;
    private static Cluster cluster;

    @BeforeClass
    public static void setup() throws Exception {
        esh = new EmbeddedServerHelper();
        esh.setup();
        cluster = HFactory.getOrCreateCluster("CounterTestCluster", new CassandraHostConfigurator("localhost:9170"));
    }

    @Test
    public void testSerializer() {
        CassandraCounterStore cassandraCounterStore =
                new CassandraCounterStore(HFactory.createKeyspace("Keyspace1", cluster));
        Count count = new Count("Counter1","k1","c1",1);
        cassandraCounterStore.save(count);
    }

    @AfterClass
    public static void teardown() throws IOException {
        EmbeddedServerHelper.teardown();
    }
}
