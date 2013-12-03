package org.apache.usergrid.persistence.collection.service.impl;


import org.apache.usergrid.persistence.collection.astynax.AstynaxKeyspaceProvider;
import org.apache.usergrid.persistence.collection.migration.Migration;
import org.apache.usergrid.persistence.collection.migration.MigrationManager;
import org.apache.usergrid.persistence.collection.migration.MigrationManagerImpl;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.impl.MvccEntitySerializationStrategyImpl;
import org.apache.usergrid.persistence.collection.serialization.impl.MvccLogEntrySerializationStrategyImpl;
import org.apache.usergrid.persistence.collection.service.TimeService;
import org.apache.usergrid.persistence.collection.service.UUIDService;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.netflix.astyanax.Keyspace;


/** @author tnine */
public class ServiceModule extends AbstractModule{

    @Override
    protected void configure() {

        //bind our keyspace to the AstynaxKeyspaceProvider
        bind( TimeService.class ).to( TimeServiceImpl.class );

        //bind our migration manager
        bind( UUIDService.class ).to( UUIDServiceImpl.class );


    }
}
