package org.apache.usergrid.persistence.collection.guice;


import java.io.File;
import java.util.Map;

import org.apache.cassandra.io.util.FileUtils;

import org.apache.usergrid.persistence.collection.migration.MigrationException;
import org.apache.usergrid.persistence.collection.migration.MigrationManager;
import org.apache.usergrid.persistence.test.AvailablePortFinder;

import com.google.common.io.Files;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.astyanax.test.EmbeddedCassandra;


public class CassandraModule extends AbstractModule {
    public static final int THRIFT_PORT = AvailablePortFinder.getNextAvailable();
    public static final int GOSSIP_PORT = AvailablePortFinder.getNextAvailable();

    private Map<String,String> overrides;
    private EmbeddedCassandra cassandra;


    public CassandraModule() throws Exception {
        super();
        File dataDir = Files.createTempDir();
        dataDir.deleteOnExit();

        if ( dataDir.exists() ) {
            FileUtils.deleteRecursive( dataDir );
        }

        cassandra = new EmbeddedCassandra( dataDir, "Usergrid", THRIFT_PORT, GOSSIP_PORT );
        cassandra.start();
    }


    public CassandraModule( Map<String,String> overrides ) {
        this.overrides = overrides;
    }

    protected void configure() {
        TestCollectionModule testCollectionModule = new TestCollectionModule( overrides );
        Injector injector = Guice.createInjector( testCollectionModule );
        MigrationManager migrationManager = injector.getInstance( MigrationManager.class );

        try {
            migrationManager.migrate();
        }
        catch ( MigrationException e ) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
