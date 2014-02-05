package org.apache.usergrid.persistence.collection.guice;


import java.io.IOException;

import com.google.inject.AbstractModule;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.SimpleDeploymentContext;


/**
 *
 *
 */
public abstract class TestModule extends AbstractModule {


    static {
                  /*
                    * --------------------------------------------------------------------
                    * Bootstrap the config for Archaius Configuration Settings.  We don't want to
                    * bootstrap more than once per JVM
                    * --------------------------------------------------------------------
                    */

        try {
            //set our deployment context to UNIT so that we load our unit tests
            SimpleDeploymentContext ctx = new SimpleDeploymentContext();
            ctx.setDeploymentEnvironment( "UNIT" );
            ConfigurationManager.setDeploymentContext( ctx );

            //load up the properties
            ConfigurationManager.loadCascadedPropertiesFromResources( "usergrid" );
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Cannot do much without properly loading our configuration.", e );
        }
    }
}
