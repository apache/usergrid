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
            setup.setup();
        } catch (Exception ex){
            logger.error("Could not setup usergrid core schema",ex);
        }
    }

    @Override
    public boolean exists() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void populateBaseData() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
