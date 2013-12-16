package org.apache.usergrid.persistence.collection.guice;


import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.migration.MigrationException;
import org.apache.usergrid.persistence.collection.migration.MigrationManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 */
@Singleton
public class MigrationManagerRule extends ExternalResource {
    private static final Logger LOG = LoggerFactory.getLogger( MigrationManagerRule.class );

    private MigrationManager migrationManager;

    // @TODO - this is not idempotent and causing issues so we make it work once
    private boolean migrated = false;


    @Inject
    public void setMigrationManager( MigrationManager migrationManager )  {
        this.migrationManager = migrationManager;
    }


    @Override
    protected void before() throws MigrationException {
        LOG.info( "Starting migration" );

        if ( ! migrated ) {
            migrationManager.migrate();
            migrated = true;
        }

        LOG.info( "Migration complete" );
    }
}
