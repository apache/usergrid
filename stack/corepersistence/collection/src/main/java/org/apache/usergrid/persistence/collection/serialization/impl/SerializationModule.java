package org.apache.usergrid.persistence.collection.serialization.impl;


import org.apache.usergrid.persistence.collection.astyanax.AstyanaxKeyspaceProvider;
import org.apache.usergrid.persistence.collection.migration.Migration;
import org.apache.usergrid.persistence.collection.migration.MigrationManager;
import org.apache.usergrid.persistence.collection.migration.MigrationManagerImpl;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.netflix.astyanax.Keyspace;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.UniqueValueSerializationStrategyImpl;


/**
 * @author tnine
 */
public class SerializationModule extends AbstractModule {


    @Override
    protected void configure() {

        // bind our keyspace to the AstyanaxKeyspaceProvider
        bind( Keyspace.class ).toProvider( AstyanaxKeyspaceProvider.class );

        // bind our migration manager
        bind( MigrationManager.class ).to( MigrationManagerImpl.class );

        // bind the serialization strategies
        bind( MvccEntitySerializationStrategy.class ).to( MvccEntitySerializationStrategyImpl.class );
        bind( MvccLogEntrySerializationStrategy.class ).to( MvccLogEntrySerializationStrategyImpl.class );
        bind( UniqueValueSerializationStrategy.class ).to( UniqueValueSerializationStrategyImpl.class );

        //do multibindings for migrations
        Multibinder<Migration> uriBinder = Multibinder.newSetBinder( binder(), Migration.class );
        uriBinder.addBinding().to( MvccEntitySerializationStrategyImpl.class );
        uriBinder.addBinding().to( MvccLogEntrySerializationStrategyImpl.class );
        uriBinder.addBinding().to( UniqueValueSerializationStrategyImpl.class );
    }
}
