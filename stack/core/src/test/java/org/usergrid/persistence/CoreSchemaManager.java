package org.usergrid.persistence;

import me.prettyprint.hector.api.Cluster;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.cassandra.SchemaManager;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.cassandra.Setup;

import javax.annotation.Resource;

/**
 * @author zznate
 */
@Ignore
public class CoreSchemaManager implements SchemaManager {
    private Logger logger = LoggerFactory.getLogger(CoreSchemaManager.class);

    private final Setup setup;
    private final Cluster cluster;

    public CoreSchemaManager(Setup setup, Cluster cluster) {
        this.setup = setup;
        this.cluster = cluster;
    }

    @Override
    public void create() {
        try {
            setup.init();
            setup.setupSystemKeyspace();
            setup.setupStaticKeyspace();

        } catch (Exception ex){
            logger.error("Could not setup usergrid core schema",ex);
        }
    }

    @Override
    public boolean exists() {
        return setup.keyspacesExist();
    }

    @Override
    public void populateBaseData() {
        try {
            setup.createDefaultApplications();
        } catch (Exception ex) {
            logger.error("Could not create default applications", ex);
        }
    }

    @Override
    public void destroy() {
        logger.info("dropping keyspaces");
        cluster.dropKeyspace(CassandraService.SYSTEM_KEYSPACE);
        cluster.dropKeyspace(CassandraService.STATIC_APPLICATION_KEYSPACE);
        logger.info("keyspaces dropped");
    }
}
