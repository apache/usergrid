package org.usergrid;


import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.usergrid.mq.QueueManagerFactory;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.IndexBucketLocator;
import org.usergrid.persistence.cassandra.CassandraService;

import java.util.UUID;


public interface CoreITSetup extends TestRule
{
    boolean USE_DEFAULT_APPLICATION = false;

    EntityManagerFactory getEmf();

    QueueManagerFactory getQmf();

    IndexBucketLocator getIbl();

    CassandraService getCassSvc();

    UUID createApplication(String organizationName, String applicationName) throws Exception;

    void dump(String name, Object obj);
}
