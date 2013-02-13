package org.usergrid.persistence;

import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.cassandra.SchemaManager;
import org.usergrid.persistence.cassandra.Setup;

import javax.annotation.Resource;

/**
 * @author zznate
 */
@Ignore
public class CoreSchemaManager implements SchemaManager {
    private Logger logger = LoggerFactory.getLogger(CoreSchemaManager.class);

    private final Setup setup;

    public CoreSchemaManager(Setup setup) {
        this.setup = setup;
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
}
