package org.apache.usergrid.persistence.queue.guice;


import org.apache.usergrid.persistence.collection.guice.TestModule;
import org.apache.usergrid.persistence.core.guice.CommonModule;



public class TestMapModule extends TestModule {

    @Override
    protected void configure() {
        install( new CommonModule());
        install( new MapModule() );
    }
}
