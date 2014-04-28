package org.apache.usergrid.persistence.core.guice;


import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.migration.MigrationException;
import org.apache.usergrid.persistence.core.migration.MigrationManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 */
@Singleton
public class MigrationManagerRule extends ExternalResource {
    private static final Logger LOG = LoggerFactory.getLogger( MigrationManagerRule.class );

    private MigrationManager migrationManager;


    @Inject
    public void setMigrationManager( MigrationManager migrationManager )  {
        this.migrationManager = migrationManager;
    }


    @Override
    protected void before() throws MigrationException {
        LOG.info( "Starting migration" );

        migrationManager.migrate();

        LOG.info( "Migration complete" );
    }
}
