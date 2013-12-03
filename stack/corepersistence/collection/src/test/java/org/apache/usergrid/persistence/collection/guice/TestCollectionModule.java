package org.apache.usergrid.persistence.collection.guice;


import java.util.HashMap;
import java.util.Map;

import org.apache.cassandra.locator.SimpleStrategy;

import org.apache.usergrid.persistence.collection.astynax.AstynaxKeyspaceProvider;
import org.apache.usergrid.persistence.collection.migration.MigrationException;
import org.apache.usergrid.persistence.collection.migration.MigrationManager;
import org.apache.usergrid.persistence.collection.migration.MigrationManagerImpl;
import org.apache.usergrid.persistence.collection.serialization.impl.MvccLogEntrySerializationStrategyImpl;
import org.apache.usergrid.persistence.test.CassandraRule;

import com.google.guiceberry.GuiceBerryEnvMain;
import com.google.guiceberry.GuiceBerryModule;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.name.Names;


/**
 * Simple module for wiring our collection api
 *
 * @author tnine
 */
public class TestCollectionModule extends AbstractModule {


    public TestCollectionModule() {
    }


    @Override
    protected void configure() {


        //import the guice berry module
        install( new GuiceBerryModule() );

        //now configure our db
        bind( GuiceBerryEnvMain.class ).to( CassAppMain.class );

        //import the runtime module
        install( new CollectionModule() );


        //configure our integration test properties. This should remain the same across all tests

        Map<String, String> configProperties = new HashMap<String, String>();
        configProperties.put( AstynaxKeyspaceProvider.CASSANDRA_HOSTS, "localhost" );
        configProperties.put( AstynaxKeyspaceProvider.CASSANDRA_PORT, "" + CassandraRule.THRIFT_PORT );
        configProperties.put( AstynaxKeyspaceProvider.CASSANDRA_CONNECTIONS, "10" );

        //time out after 5 seconds
        configProperties.put( AstynaxKeyspaceProvider.CASSANDRA_TIMEOUT, "5000" );
        configProperties.put( AstynaxKeyspaceProvider.CASSANDRA_CLUSTER_NAME, "Usergrid" );
        configProperties.put( AstynaxKeyspaceProvider.CASSANDRA_VERSION, "1.2" );
        configProperties.put( AstynaxKeyspaceProvider.COLLECTIONS_KEYSPACE_NAME, "Usergrid_Collections" );

        configProperties.put( MigrationManagerImpl.REPLICATION_FACTOR, "1" );
        configProperties.put( MigrationManagerImpl.STRATEGY_CLASS, SimpleStrategy.class.getName() );

        /**
         * Set the timeout to 60 seconds, no test should take that long for load+delete without a failure
         */
        configProperties.put( MvccLogEntrySerializationStrategyImpl.TIMEOUT_PROP, 60+"" );

        Map<String, String> props = getOverrides();

        if(props != null){
            configProperties.putAll( props );
        }

        //bind to the props
        Names.bindProperties( binder(), configProperties );
    }


    /**
     * Get any overrides we need for system properties
     */
    public Map<String, String> getOverrides() {
        return null;
    }


    static class CassAppMain implements GuiceBerryEnvMain {

        @Inject
        protected MigrationManager migrationManager;


        public void run() {
            try {
                //run the injected migration manager to set up cassandra
                migrationManager.migrate();
            }
            catch ( MigrationException e ) {
                throw new RuntimeException( e );
            }
        }
    }
}
