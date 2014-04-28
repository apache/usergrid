package org.apache.usergrid.persistence.core.guice;


import org.safehaus.guicyfig.GuicyFigModule;

import org.apache.usergrid.persistence.core.astyanax.AstyanaxKeyspaceProvider;
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.core.migration.MigrationManager;
import org.apache.usergrid.persistence.core.migration.MigrationManagerFig;
import org.apache.usergrid.persistence.core.migration.MigrationManagerImpl;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.netflix.astyanax.Keyspace;


/**
 * Simple module for configuring our core services.  Cassandra etc
 *
 * @author tnine
 */
public class CoreModule extends AbstractModule {


    @Override
    protected void configure() {
        //noinspection unchecked
        install( new GuicyFigModule(
                MigrationManagerFig.class,
                CassandraFig.class) );

             // bind our keyspace to the AstyanaxKeyspaceProvider
        bind( Keyspace.class ).toProvider( AstyanaxKeyspaceProvider.class );

        // bind our migration manager
        bind( MigrationManager.class ).to( MigrationManagerImpl.class );


    }
}
