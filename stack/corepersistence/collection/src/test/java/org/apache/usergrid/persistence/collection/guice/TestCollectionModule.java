package org.apache.usergrid.persistence.collection.guice;


import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.cassandra.locator.SimpleStrategy;

import org.apache.usergrid.persistence.collection.archaius.DynamicPropertyNames;
import org.apache.usergrid.persistence.collection.astynax.AstynaxKeyspaceProvider;
import org.apache.usergrid.persistence.collection.migration.MigrationManagerImpl;
import org.apache.usergrid.persistence.collection.rx.CassandraThreadScheduler;
import org.apache.usergrid.persistence.collection.serialization.impl.MvccLogEntrySerializationStrategyImpl;
import org.apache.usergrid.persistence.test.CassandraRule;

import com.google.guiceberry.GuiceBerryModule;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;


/**
 * This module manually constructs Properties specific to the unit testing environment and then it overlays
 * overrides (if not null). These properties are used to build Named bindings to inject configuration parameters
 * into classes like the {@link AstynaxKeyspaceProvider}.
 *
 * It also installs the GuiceBerryModule, and the CollectionModule.
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

        Properties configProperties = new Properties();
        configProperties.put( AstynaxKeyspaceProvider.CASSANDRA_HOSTS, "localhost" );
        configProperties.put( AstynaxKeyspaceProvider.CASSANDRA_PORT, "" + CassandraRule.THRIFT_PORT );
        configProperties.put( AstynaxKeyspaceProvider.CASSANDRA_CONNECTIONS, "10" );

        //time out after 5 seconds
        configProperties.put( AstynaxKeyspaceProvider.CASSANDRA_TIMEOUT, "5000" );
        configProperties.put( AstynaxKeyspaceProvider.CASSANDRA_CLUSTER_NAME, "Usergrid" );
        configProperties.put( AstynaxKeyspaceProvider.CASSANDRA_VERSION + ".String", "1.2" );
        configProperties.put( AstynaxKeyspaceProvider.COLLECTIONS_KEYSPACE_NAME, "Usergrid_Collections" );
        configProperties.put( CassandraThreadScheduler.RX_IO_THREADS, "20" );

        if ( override != null ) {
            configProperties.putAll( override );
        }

        //bind to the props
        DynamicPropertyNames.bindProperties( binder(), configProperties );

        // ======

        configProperties.clear();
        configProperties.put( MigrationManagerImpl.REPLICATION_FACTOR, "1" );
        configProperties.put( MigrationManagerImpl.STRATEGY_CLASS, SimpleStrategy.class.getName() );

        /**
         * Set the timeout to 60 seconds, no test should take that long for load+delete without a failure
         */
        configProperties.put( MvccLogEntrySerializationStrategyImpl.TIMEOUT_PROP, "60" );

        if ( override != null ) {
            configProperties.putAll( override );
        }

        Names.bindProperties( binder(), configProperties );
    }
}
