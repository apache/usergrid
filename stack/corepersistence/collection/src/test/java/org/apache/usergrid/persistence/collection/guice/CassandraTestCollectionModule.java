package org.apache.usergrid.persistence.collection.guice;


import java.util.Map;

import org.apache.usergrid.persistence.collection.migration.MigrationException;
import org.apache.usergrid.persistence.collection.migration.MigrationManager;

import com.google.guiceberry.GuiceBerryEnvMain;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;


/**
 * Simple module for wiring our collection api
 *
 * @author tnine
 */
public class CassandraTestCollectionModule extends AbstractModule {

    private final Map<String, String> overrides;


    public CassandraTestCollectionModule( final Map<String, String> overrides ) {
        this.overrides = overrides;
    }


    public CassandraTestCollectionModule() {
        this.overrides = null;
    }


    @Override
    protected void configure() {


        //import the guice berry module
        install( new TestCollectionModule( overrides ) );


        //now configure our db
        bind( GuiceBerryEnvMain.class ).to( CassAppMain.class );
    }


    static class CassAppMain implements GuiceBerryEnvMain {

        @Inject
        protected MigrationManager migrationManager;


        public void run() {
            try {
                //run the injected migration manager to set up cassandra
                migrationManager.migrate();
            }
            catch ( MigrationException e ) {
                throw new RuntimeException( e );
            }
        }
    }
}
