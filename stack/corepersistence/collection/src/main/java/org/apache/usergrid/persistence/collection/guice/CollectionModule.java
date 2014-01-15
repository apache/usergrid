package org.apache.usergrid.persistence.collection.guice;


import java.io.IOException;

import org.safehaus.guicyfig.Env;
import org.safehaus.guicyfig.GuicyFigModule;

import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerSync;
import org.apache.usergrid.persistence.collection.astyanax.CassandraFig;
import org.apache.usergrid.persistence.collection.cassandra.AvailablePortFinder;
import org.apache.usergrid.persistence.collection.impl.EntityCollectionManagerImpl;
import org.apache.usergrid.persistence.collection.impl.EntityCollectionManagerSyncImpl;
import org.apache.usergrid.persistence.collection.migration.MigrationManagerFig;
import org.apache.usergrid.persistence.collection.rx.CassandraThreadScheduler;
import org.apache.usergrid.persistence.collection.rx.RxFig;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.impl.SerializationModule;
import org.apache.usergrid.persistence.collection.service.impl.ServiceModule;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.netflix.config.ConfigurationManager;

import rx.Scheduler;


/**
 * Simple module for wiring our collection api
 *
 * @author tnine
 */
public class CollectionModule extends AbstractModule {

    static {
        /*
         * --------------------------------------------------------------------
         * Archaius Configuration Settings
         * --------------------------------------------------------------------
         */

        try {
            ConfigurationManager.loadCascadedPropertiesFromResources( "usergrid" );
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Cannot do much without properly loading our configuration.", e );
        }

        Injector injector = Guice.createInjector( new GuicyFigModule( CassandraFig.class, RxFig.class ) );
        CassandraFig cassandraFig = injector.getInstance( CassandraFig.class );
        RxFig rxFig = injector.getInstance( RxFig.class );

        Env env = Env.getEnvironment();
        if ( env == Env.UNIT || env == Env.ALL ) {
            String thriftPort = String.valueOf( AvailablePortFinder.getNextAvailable() );
            cassandraFig.bypass( "getThriftPort", thriftPort );
            cassandraFig.bypass( "getConnections", "20" );
            rxFig.bypass( "getMaxThreadCount", "20" );
            cassandraFig.bypass( "getHosts", "localhost" );
            cassandraFig.bypass( "getClusterName", "Usergrid" );
            cassandraFig.bypass( "getKeyspaceName", "Usergrid_Collections" );
        }
    }

    @Override
    protected void configure() {
        //noinspection unchecked
        install( new GuicyFigModule( RxFig.class, MigrationManagerFig.class,
                CassandraFig.class, SerializationFig.class ) );

        install( new SerializationModule() );
        install( new ServiceModule() );

        // create a guice factor for getting our collection manager
        install( new FactoryModuleBuilder()
                .implement( EntityCollectionManager.class, EntityCollectionManagerImpl.class )
                .implement( EntityCollectionManagerSync.class, EntityCollectionManagerSyncImpl.class )
                .build( EntityCollectionManagerFactory.class ) );

        bind( Scheduler.class ).toProvider( CassandraThreadScheduler.class );
    }
}
