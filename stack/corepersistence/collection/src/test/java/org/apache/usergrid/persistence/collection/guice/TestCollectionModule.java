package org.apache.usergrid.persistence.collection.guice;


import java.util.HashMap;
import java.util.Map;

import org.apache.cassandra.locator.SimpleStrategy;

import org.apache.usergrid.persistence.collection.astynax.AstynaxKeyspaceProvider;
import org.apache.usergrid.persistence.collection.migration.MigrationManagerImpl;
import org.apache.usergrid.persistence.collection.serialization.impl.MvccLogEntrySerializationStrategyImpl;
import org.apache.usergrid.persistence.test.CassandraRule;

import com.google.guiceberry.GuiceBerryModule;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;


/**
 * Module for testing our guice wiring environment is correct. Does not actually set the main execution env. Callers are
 * responsible for that via decoration
 *
 * @author tnine
 */
public class TestCollectionModule extends AbstractModule {


    private final Map<String, String> override;


    public TestCollectionModule( Map<String, String> override ) {
        this.override = override;
    }


    public TestCollectionModule() {
        override = null;
    }


    @Override
    protected void configure() {


        //import the guice berry module
        install( new GuiceBerryModule() );

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
        configProperties.put( MvccLogEntrySerializationStrategyImpl.TIMEOUT_PROP, 60 + "" );


        if(override != null){
            configProperties.putAll( override );
        }

        //bind to the props
        Names.bindProperties( binder(), configProperties );
    }
}
