package org.apache.usergrid.persistence.core.guice;


import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.migration.data.DataMigrationManager;
import org.apache.usergrid.persistence.core.migration.schema.MigrationException;
import org.apache.usergrid.persistence.core.migration.schema.MigrationManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 */
@Singleton
public class MigrationManagerRule extends ExternalResource {
    private static final Logger logger = LoggerFactory.getLogger( MigrationManagerRule.class );


    private MigrationManager migrationManager;
    private DataMigrationManager dataMigrationManager;


    @Inject
    public void setMigrationManager( final MigrationManager migrationManager )  {
        this.migrationManager = migrationManager;

        try {
                   this.migrationManager.migrate();
               }
               catch ( MigrationException e ) {
                   throw new RuntimeException(e);
               }
    }

    @Inject
    public void setDataMigrationManager(final DataMigrationManager dataMigrationManager){
        this.dataMigrationManager = dataMigrationManager;
    }

    @Override
    protected void before() throws MigrationException {
        logger.info( "Starting migration" );

        migrationManager.migrate();

        logger.info("Migrating data");

        dataMigrationManager.migrate();

        logger.info( "Migration complete" );
    }
}
