package org.apache.usergrid.persistence.collection.guice;


import java.util.Collections;
import java.util.Map;

import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerSync;
import org.apache.usergrid.persistence.collection.cassandra.CassandraConfigModule;
import org.apache.usergrid.persistence.collection.cassandra.IDynamicCassandraConfig;
import org.apache.usergrid.persistence.collection.impl.EntityCollectionManagerImpl;
import org.apache.usergrid.persistence.collection.impl.EntityCollectionManagerSyncImpl;
import org.apache.usergrid.persistence.collection.serialization.impl.SerializationModule;
import org.apache.usergrid.persistence.collection.service.impl.ServiceModule;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConcurrentMapConfiguration;
import com.netflix.config.ConfigurationManager;


/**
 * Simple module for wiring our collection api
 *
 * @author tnine
 */
public class CollectionModule extends AbstractModule {
    private final Map<String,Object> overrides;


    public CollectionModule( Map<String,Object> overrides ) {
        this.overrides = overrides;
    }


    public CollectionModule() {
        this.overrides = Collections.emptyMap();
    }


    @Override
    protected void configure() {
        // We have to access the cassandra configuration to set the thread scheduler
        // configuration below
        CassandraConfigModule cassandraConfigModule = new CassandraConfigModule( overrides );
        Injector injector = Guice.createInjector( cassandraConfigModule );
        IDynamicCassandraConfig cassandraConfig = injector.getInstance( IDynamicCassandraConfig.class );

        //TODO can this be refactored into the core? Both graph and collection depend on the cass config, as well as rx configuration

        if ( ConfigurationManager.getConfigInstance() instanceof ConcurrentCompositeConfiguration ) {
            ConcurrentCompositeConfiguration config =
                    ( ConcurrentCompositeConfiguration ) ConfigurationManager.getConfigInstance();

            //noinspection unchecked
            ConcurrentMapConfiguration mapConfiguration = new ConcurrentMapConfiguration( ( Map ) overrides );


            config.addConfigurationAtFront( mapConfiguration, "CollectionModule" );
        }

        install( cassandraConfigModule );
        install( new SerializationModule( overrides ) );
        install( new ServiceModule() );

        // create a guice factor for getting our collection manager
        install( new FactoryModuleBuilder()
                .implement( EntityCollectionManager.class, EntityCollectionManagerImpl.class )
                .implement( EntityCollectionManagerSync.class, EntityCollectionManagerSyncImpl.class )
                .build( EntityCollectionManagerFactory.class ) );


    }
}
