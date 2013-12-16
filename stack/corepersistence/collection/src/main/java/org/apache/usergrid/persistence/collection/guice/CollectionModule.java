package org.apache.usergrid.persistence.collection.guice;


import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerSync;
import org.apache.usergrid.persistence.collection.cassandra.CassandraConfigModule;
import org.apache.usergrid.persistence.collection.impl.EntityCollectionManagerImpl;
import org.apache.usergrid.persistence.collection.impl.EntityCollectionManagerSyncImpl;
import org.apache.usergrid.persistence.collection.rx.CassandraThreadScheduler;
import org.apache.usergrid.persistence.collection.serialization.impl.SerializationModule;
import org.apache.usergrid.persistence.collection.service.impl.ServiceModule;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

import rx.Scheduler;


/**
 * Simple module for wiring our collection api
 *
 * @author tnine
 */
public class CollectionModule extends AbstractModule {
    @Override
    protected void configure() {
        install( new CassandraConfigModule() );
        install( new SerializationModule() );
        install( new ServiceModule() );

        // create a guice factor for getting our collection manager
        install( new FactoryModuleBuilder()
                .implement( EntityCollectionManager.class, EntityCollectionManagerImpl.class )
                .implement( EntityCollectionManagerSync.class, EntityCollectionManagerSyncImpl.class )
                .build( EntityCollectionManagerFactory.class ) );

        // bind our RX scheduler
        bind( Scheduler.class).toProvider(CassandraThreadScheduler.class);
    }
}
