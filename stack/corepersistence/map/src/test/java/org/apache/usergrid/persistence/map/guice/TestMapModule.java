package org.apache.usergrid.persistence.map.guice;


import org.apache.usergrid.persistence.core.guice.TestModule;
import org.apache.usergrid.persistence.core.guice.CommonModule;



public class TestMapModule extends TestModule {

    @Override
    protected void configure() {
        install( new CommonModule());
        install( new MapModule() );
    }
}
