package org.apache.usergrid.persistence.collection.guice;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.locator.SimpleStrategy;

import org.apache.usergrid.persistence.collection.astynax.AstynaxKeyspaceProvider;
import org.apache.usergrid.persistence.collection.cassandra.ICassandraConfig;
import org.apache.usergrid.persistence.collection.migration.MigrationManagerImpl;
import org.apache.usergrid.persistence.collection.rx.CassandraThreadScheduler;
import org.apache.usergrid.persistence.collection.serialization.impl.MvccLogEntrySerializationStrategyImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;


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
    private static final Logger LOG = LoggerFactory.getLogger( TestCollectionModule.class );
    private final Map<String, String> override;


    /**
     * Our RX I/O threads and this should have the same value
     */
    private static final String CONNECTION_COUNT = "20";

    public TestCollectionModule( Map<String, String> override ) {
        this.override = new HashMap<String, String>();
        this.override.putAll( override );
    }


    @SuppressWarnings( "UnusedDeclaration" )
    public TestCollectionModule() {
        override = Collections.emptyMap();
    }


    @Override
    protected void configure() {
        Map<String,Object> propMap = new HashMap<String, Object>();
        propMap.put( ICassandraConfig.CASSANDRA_HOSTS, "localhost" );
        propMap.put( ICassandraConfig.CASSANDRA_PORT, "" + CassandraRule.THRIFT_PORT );
        propMap.put( ICassandraConfig.CASSANDRA_CONNECTIONS, CONNECTION_COUNT );
        propMap.put( ICassandraConfig.CASSANDRA_TIMEOUT, "5000" );
        propMap.put( ICassandraConfig.CASSANDRA_CLUSTER_NAME, "Usergrid" );
        propMap.put( ICassandraConfig.CASSANDRA_VERSION, "1.2" );
        propMap.put( ICassandraConfig.COLLECTIONS_KEYSPACE_NAME, "Usergrid_Collections" );

        propMap.put( MigrationManagerImpl.REPLICATION_FACTOR, "1" );
        propMap.put( MigrationManagerImpl.STRATEGY_CLASS, SimpleStrategy.class.getName() );

        propMap.put( CassandraThreadScheduler.RX_IO_THREADS, CONNECTION_COUNT );

        propMap.put( MvccLogEntrySerializationStrategyImpl.TIMEOUT_PROP, "60" );

        propMap.putAll( override );
        install( new CollectionModule( propMap ) );
    }



}
