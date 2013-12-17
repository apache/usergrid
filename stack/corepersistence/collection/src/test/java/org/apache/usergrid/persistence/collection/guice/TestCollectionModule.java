package org.apache.usergrid.persistence.collection.guice;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.locator.SimpleStrategy;

import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerSync;
import org.apache.usergrid.persistence.collection.archaius.DynamicPropertyNames;
import org.apache.usergrid.persistence.collection.astynax.AstynaxKeyspaceProvider;
import org.apache.usergrid.persistence.collection.cassandra.CassandraConfigModule;
import org.apache.usergrid.persistence.collection.cassandra.ICassandraConfig;
import org.apache.usergrid.persistence.collection.impl.EntityCollectionManagerImpl;
import org.apache.usergrid.persistence.collection.impl.EntityCollectionManagerSyncImpl;
import org.apache.usergrid.persistence.collection.migration.MigrationManagerImpl;
import org.apache.usergrid.persistence.collection.rx.CassandraThreadScheduler;
import org.apache.usergrid.persistence.collection.serialization.impl.MvccLogEntrySerializationStrategyImpl;
import org.apache.usergrid.persistence.collection.serialization.impl.SerializationModule;
import org.apache.usergrid.persistence.collection.service.impl.ServiceModule;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.netflix.astyanax.Keyspace;
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConcurrentMapConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.util.ConfigurationUtils;

import rx.Scheduler;


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
        this.override = override;
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
        propMap.put( ICassandraConfig.CASSANDRA_VERSION + ".String", "1.2" );
        propMap.put( ICassandraConfig.COLLECTIONS_KEYSPACE_NAME, "Usergrid_Collections" );
        propMap.putAll( override );

        if ( ConfigurationManager.getConfigInstance() instanceof ConcurrentCompositeConfiguration ) {
            ConcurrentCompositeConfiguration config =
                    ( ConcurrentCompositeConfiguration ) ConfigurationManager.getConfigInstance();

            ConcurrentMapConfiguration mapConfiguration = new ConcurrentMapConfiguration( propMap );
            config.addConfigurationAtFront( mapConfiguration, "testConfig" );
        }

        Properties props = new Properties();
        props.putAll( propMap );
        install( new CassandraConfigModule( props ) );

        // =================== Cassandra Configuration Done ===================
        // ====================================================================

        propMap.clear();
        propMap.put( CassandraThreadScheduler.RX_IO_THREADS, CONNECTION_COUNT );

        if ( override != null ) {
            propMap.putAll( override );
        }

        //bind to the props
        props.clear();
        props.putAll( propMap );
        DynamicPropertyNames.bindProperties( binder(), props );

        // ======

        //configProperties.clear();
        propMap.put( MigrationManagerImpl.REPLICATION_FACTOR, "1" );
        propMap.put( MigrationManagerImpl.STRATEGY_CLASS, SimpleStrategy.class.getName() );

        /**
         * Set the timeout to 60 seconds, no test should take that long for load+delete without a failure
         */
        propMap.put( MvccLogEntrySerializationStrategyImpl.TIMEOUT_PROP, "60" );

        if ( override != null ) {
            propMap.putAll( override );
        }

        props.clear();
        props.putAll( propMap );
        Names.bindProperties( binder(), props );

        install( new CollectionModule() );

        //bind our listener to start cassandra

        bindListener( Matchers.any(), new KeyspaceListener ());
    }

    private static class KeyspaceListener implements TypeListener{

        @Override
        public <I> void hear( final TypeLiteral<I> type, final TypeEncounter<I> encounter ) {
            if(type.getType() == AstynaxKeyspaceProvider.class){
                CassandraRule cass = new CassandraRule();
                try {
                    cass.before();
                }
                catch ( Throwable t ) {
                    throw new RuntimeException( "Something nasty happened!", t );
                }
            }
        }
    }



}
