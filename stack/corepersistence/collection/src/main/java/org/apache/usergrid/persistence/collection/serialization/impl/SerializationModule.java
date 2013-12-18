package org.apache.usergrid.persistence.collection.serialization.impl;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.usergrid.persistence.collection.astynax.AstynaxKeyspaceProvider;
import org.apache.usergrid.persistence.collection.migration.Migration;
import org.apache.usergrid.persistence.collection.migration.MigrationManager;
import org.apache.usergrid.persistence.collection.migration.MigrationManagerImpl;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.netflix.astyanax.Keyspace;
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConcurrentMapConfiguration;
import com.netflix.config.ConfigurationManager;

import static org.apache.usergrid.persistence.collection.guice.PropertyUtils.filter;
import static org.apache.usergrid.persistence.collection.guice.PropertyUtils.loadFromClassPath;


/**
 * @author tnine
 */
public class SerializationModule extends AbstractModule {
    private static final String[] OPTIONS = {
        MvccLogEntrySerializationStrategyImpl.TIMEOUT_PROP,
        MigrationManagerImpl.STRATEGY_CLASS,
        MigrationManagerImpl.REPLICATION_FACTOR,
        MigrationManagerImpl.STRATEGY_OPTIONS
    };
    private final Map<String,Object> overrides;


    public SerializationModule() {
        overrides = Collections.emptyMap();
    }


    public SerializationModule( Map<String,Object> overrides ) {
        this.overrides = new HashMap<String, Object>();
        this.overrides.putAll( filter( OPTIONS, overrides ) );
    }


    @Override
    protected void configure() {
        // NOTE : this should be replaced because the defaults do no work the same when
        // dynamic properties are used
        // Bind all the defaults for named properties
        Properties properties = loadFromClassPath( "serialization-defaults.properties" );
        properties.putAll( overrides );
        Names.bindProperties( binder(), properties );   // need to filter to prevent double bind

        if ( ConfigurationManager.getConfigInstance() instanceof ConcurrentCompositeConfiguration ) {
            ConcurrentCompositeConfiguration config =
                    ( ConcurrentCompositeConfiguration ) ConfigurationManager.getConfigInstance();
            ConcurrentMapConfiguration mapConfiguration = new ConcurrentMapConfiguration( overrides );
            config.addConfigurationAtFront( mapConfiguration, "serializationModuleConfig" );
        }

        // bind our keyspace to the AstynaxKeyspaceProvider
        bind( Keyspace.class ).toProvider( AstynaxKeyspaceProvider.class );

        // bind our migration manager
        bind( MigrationManager.class ).to( MigrationManagerImpl.class );

        // bind the serialization strategies
        bind( MvccEntitySerializationStrategy.class ).to( MvccEntitySerializationStrategyImpl.class );
        bind( MvccLogEntrySerializationStrategy.class ).to( MvccLogEntrySerializationStrategyImpl.class );

        //do multibindings for migrations
        Multibinder<Migration> uriBinder = Multibinder.newSetBinder( binder(), Migration.class );
        uriBinder.addBinding().to( MvccEntitySerializationStrategyImpl.class );
        uriBinder.addBinding().to( MvccLogEntrySerializationStrategyImpl.class );
    }
}
