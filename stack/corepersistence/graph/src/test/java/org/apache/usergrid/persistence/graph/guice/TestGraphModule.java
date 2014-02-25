package org.apache.usergrid.persistence.graph.guice;


import org.apache.usergrid.persistence.collection.guice.CollectionModule;
import org.apache.usergrid.persistence.collection.guice.TestModule;


/**
 * Wrapper for configuring our guice test env
 *
 */
public class TestGraphModule extends TestModule {



    @Override
    protected void configure() {
        install(new GraphModule());
    }
}
