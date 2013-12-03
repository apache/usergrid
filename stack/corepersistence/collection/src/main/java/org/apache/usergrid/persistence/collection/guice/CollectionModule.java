package org.apache.usergrid.persistence.collection.guice;


import java.util.Properties;

import org.apache.usergrid.persistence.collection.astynax.AstynaxKeyspaceProvider;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.CollectionPipelineModule;
import org.apache.usergrid.persistence.collection.serialization.impl.SerializationModule;
import org.apache.usergrid.persistence.collection.service.impl.ServiceModule;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;


/**
 * Simple module for wiring our collection api
 *
 * @author tnine
 */
public class CollectionModule extends AbstractModule {

    /** The location of the properties file */
    private static final String CASS_PROPS = "cassandra.properties";


    @Override
    protected void configure() {

        /**
         * Load our properties for the entire colleciton module
         *
         */

        //bind our cassandra properties

        Properties props = PropertyUtils.loadFromClassPath( CASS_PROPS );

        Names.bindProperties( binder(), props );

        //Load the cassandra url if set on the system properties
        Names.bindProperties( binder(),
                PropertyUtils.loadSystemProperties( AstynaxKeyspaceProvider.getRuntimeOptions() ) );


        //TODO allow override of all properties in the file by the system


        /**
         * Install the write pipeline configuration
         */
        install( new CollectionPipelineModule() );

        //Install serialization modules
        install( new SerializationModule());

        install (new ServiceModule());
    }
}
