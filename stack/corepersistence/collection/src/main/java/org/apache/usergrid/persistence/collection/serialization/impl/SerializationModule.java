package org.apache.usergrid.persistence.collection.serialization.impl;


import org.apache.usergrid.persistence.collection.astynax.AstynaxKeyspaceProvider;
import org.apache.usergrid.persistence.collection.astynax.CassandraFig;
import org.apache.usergrid.persistence.collection.migration.Migration;
import org.apache.usergrid.persistence.collection.migration.MigrationManager;
import org.apache.usergrid.persistence.collection.migration.MigrationManagerFig;
import org.apache.usergrid.persistence.collection.migration.MigrationManagerImpl;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.netflix.astyanax.Keyspace;


/**
 * @author tnine
 */
public class SerializationModule extends AbstractModule {
    private final CassandraFig cassandraFig;
    private final MigrationManagerFig migrationManagerFig;
    private final SerializationFig serializationFig;

    public SerializationModule( CassandraFig cassandraFig, MigrationManagerFig migrationFig,
                                SerializationFig serializationFig ) {
        this.cassandraFig = cassandraFig;
        this.migrationManagerFig = migrationFig;
        this.serializationFig = serializationFig;
    }


    @Override
    protected void configure() {
        bind( CassandraFig.class ).toInstance( cassandraFig );
        bind( MigrationManagerFig.class ).toInstance( migrationManagerFig );
        bind( SerializationFig.class ).toInstance( serializationFig );

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
