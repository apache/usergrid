package org.apache.usergrid.persistence.map.guice;


import org.apache.usergrid.persistence.core.guice.CommonModule;
import org.apache.usergrid.persistence.core.guice.TestModule;


public class TestMapModule extends TestModule {

    @Override
    protected void configure() {
        install( new CommonModule());
        install( new MapModule() );
    }
}
