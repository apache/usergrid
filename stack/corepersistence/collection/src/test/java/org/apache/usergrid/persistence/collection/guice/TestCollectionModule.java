package org.apache.usergrid.persistence.collection.guice;


import java.io.IOException;

import org.safehaus.guicyfig.Env;

import com.google.inject.AbstractModule;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.SimpleDeploymentContext;


/**
 *
 *
 */
public class TestCollectionModule extends TestModule {



    @Override
    protected void configure() {
        install(new CollectionModule());
    }
}
