package org.apache.usergrid.persistence.collection.guice;


import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerSync;
import org.apache.usergrid.persistence.collection.astynax.CassandraFig;
import org.apache.usergrid.persistence.collection.impl.EntityCollectionManagerImpl;
import org.apache.usergrid.persistence.collection.impl.EntityCollectionManagerSyncImpl;
import org.apache.usergrid.persistence.collection.migration.MigrationManagerFig;
import org.apache.usergrid.persistence.collection.rx.CassandraThreadScheduler;
import org.apache.usergrid.persistence.collection.rx.RxFig;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
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
    private final CassandraFig cassandraFig;
    private final MigrationManagerFig migrationFig;
    private final SerializationFig serializationFig;
    private final RxFig rxFig;


    public CollectionModule( CassandraFig cassandraFig, MigrationManagerFig migrationFig,
                             SerializationFig serializationFig, RxFig rxFig ) {
        this.cassandraFig = cassandraFig;
        this.migrationFig = migrationFig;
        this.serializationFig = serializationFig;
        this.rxFig = rxFig;
    }


    @Override
    protected void configure() {
        bind( RxFig.class ).toInstance( rxFig );
        install( new SerializationModule( cassandraFig, migrationFig, serializationFig  ) );
        install( new ServiceModule() );

        // create a guice factor for getting our collection manager
        install( new FactoryModuleBuilder()
                .implement( EntityCollectionManager.class, EntityCollectionManagerImpl.class )
                .implement( EntityCollectionManagerSync.class, EntityCollectionManagerSyncImpl.class )
                .build( EntityCollectionManagerFactory.class ) );

        bind( Scheduler.class ).toProvider( CassandraThreadScheduler.class );
    }
}
