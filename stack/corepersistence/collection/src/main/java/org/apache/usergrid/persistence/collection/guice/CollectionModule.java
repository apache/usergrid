package org.apache.usergrid.persistence.collection.guice;


import java.util.Properties;

import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.archaius.DynamicPropertyNames;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerSync;
import org.apache.usergrid.persistence.collection.astynax.AstynaxKeyspaceProvider;
import org.apache.usergrid.persistence.collection.impl.EntityCollectionManagerImpl;
import org.apache.usergrid.persistence.collection.impl.EntityCollectionManagerSyncImpl;
import org.apache.usergrid.persistence.collection.serialization.impl.SerializationModule;
import org.apache.usergrid.persistence.collection.service.impl.ServiceModule;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;


/**
 * Simple module for wiring our collection api
 *
 * @author tnine
 */
public class CollectionModule extends AbstractModule {

    /**
     * The location of the properties file
     */
    private static final String CASS_PROPS = "cassandra.properties";


    @Override
    protected void configure() {

        /**
         * Load our properties for the entire colleciton module
         *
         */

        //bind our cassandra properties

        Properties props = PropertyUtils.loadFromClassPath( CASS_PROPS );
        DynamicPropertyNames.bindProperties( binder(), props );

        //Load the cassandra url if set on the system properties
        DynamicPropertyNames.bindProperties( binder(), PropertyUtils
                .loadSystemProperties( AstynaxKeyspaceProvider.getRuntimeOptions() ) );


        //TODO allow override of all properties in the file by the system


        //Install serialization modules
        install( new SerializationModule() );

        install( new ServiceModule() );

        //install the core services

        //create a guice factor for getting our collection manager
        install( new FactoryModuleBuilder().implement( EntityCollectionManager.class, EntityCollectionManagerImpl.class )
                        .implement( EntityCollectionManagerSync.class, EntityCollectionManagerSyncImpl.class )
                                          .build( EntityCollectionManagerFactory.class ) );

    }
}
