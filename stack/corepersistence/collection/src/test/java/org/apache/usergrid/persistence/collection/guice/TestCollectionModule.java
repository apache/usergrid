package org.apache.usergrid.persistence.collection.guice;


import org.safehaus.guicyfig.GuicyFigModule;
import org.safehaus.guicyfig.Option;
import org.safehaus.guicyfig.Overrides;

import org.apache.usergrid.persistence.collection.astynax.CassandraFig;
import org.apache.usergrid.persistence.collection.migration.MigrationManagerFig;
import org.apache.usergrid.persistence.collection.rx.RxFig;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;


/**
 * This module manually constructs configs specific to the unit testing environment and then it
 * overrides (if not null).
 */
public class TestCollectionModule extends AbstractModule {
    /** Our RX I/O threads and this should have the same value */
    private static final String CONNECTION_COUNT = "20";

    @Inject
    @Overrides(
        name = "unit-test",
        contexts = Overrides.Env.unit,
        options = {
                @Option( method = "getHosts", override = "localhost" ),
                @Option( method = "getConnections", override = CONNECTION_COUNT )
        }
    )
    CassandraFig cassandraFig;

    @Inject
    @Overrides( name = "unit-test", options = @Option( method = "getMaxThreadCount", override = CONNECTION_COUNT ) )
    RxFig rxFig;

    @Inject
    SerializationFig serializationFig;

    @Inject
    MigrationManagerFig migrationManagerFig;


    public SerializationFig getSerializationFig() {
        return serializationFig;
    }


    public TestCollectionModule() {
        //noinspection unchecked
        Injector injector = Guice.createInjector( new GuicyFigModule(
            RxFig.class, CassandraFig.class, SerializationFig.class, MigrationManagerFig.class
        ) );
        injector.injectMembers( this );

        Preconditions.checkNotNull( cassandraFig );
        Preconditions.checkNotNull( rxFig );
        Preconditions.checkNotNull( migrationManagerFig );
        Preconditions.checkNotNull( serializationFig );
    }


    @Override
    protected void configure() {
        bind( CassandraFig.class ).toInstance( cassandraFig );
        bind( RxFig.class ).toInstance( rxFig );
        bind( SerializationFig.class ).toInstance( serializationFig );
        bind( MigrationManagerFig.class ).toInstance( migrationManagerFig );

        // this is dynamic so we cannot use annotations to get the port
//        cassandraFig.getOption( cassandraFig.getKeyByMethod( "getPort" ) )
//                    .setOverride( Integer.toString( CassandraRule.THRIFT_PORT ) );

        install( new CollectionModule( cassandraFig, migrationManagerFig, serializationFig, rxFig ) );
    }
}
