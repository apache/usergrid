package org.apache.usergrid.persistence.collection.guice;


import org.apache.usergrid.persistence.collection.astynax.AstynaxKeyspaceProvider;
import org.apache.usergrid.persistence.collection.migration.Migration;
import org.apache.usergrid.persistence.collection.migration.MigrationManager;
import org.apache.usergrid.persistence.collection.migration.MigrationManagerImpl;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategyImpl;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategyImpl;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.netflix.astyanax.Keyspace;


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


        //bind our cassandra properties
        Names.bindProperties( binder(), PropertyUtils.loadFromClassPath( CASS_PROPS ) );

        //Load the cassandra url if set on the system properties
        Names.bindProperties( binder(),
                PropertyUtils.loadSystemProperties( AstynaxKeyspaceProvider.getRuntimeOptions() ) );

        //bind our keyspace to the AstynaxKeyspaceProvider
        bind( Keyspace.class ).toProvider( AstynaxKeyspaceProvider.class );

        //bind our migration manager
        bind(MigrationManager.class).to( MigrationManagerImpl.class );


        //bind the serialization strategies

        bind( MvccEntitySerializationStrategy.class ).to( MvccEntitySerializationStrategyImpl.class );


        bind( MvccLogEntrySerializationStrategy.class ).to( MvccLogEntrySerializationStrategyImpl.class );


        //do multibindings for migrations
        Multibinder<Migration> uriBinder = Multibinder.newSetBinder( binder(), Migration.class );

        uriBinder.addBinding().to( MvccEntitySerializationStrategyImpl.class );
        uriBinder.addBinding().to( MvccLogEntrySerializationStrategyImpl.class );
    }
}
