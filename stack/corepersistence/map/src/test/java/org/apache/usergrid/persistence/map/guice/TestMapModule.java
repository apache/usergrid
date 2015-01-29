package org.apache.usergrid.persistence.map.guice;


import org.apache.usergrid.persistence.core.guice.TestModule;
import org.apache.usergrid.persistence.core.guice.CommonModule;
import org.apache.usergrid.persistence.core.rx.AllEntitiesInSystemObservable;
import org.apache.usergrid.persistence.core.rx.AllEntitiesInSystemTestObservable;


public class TestMapModule extends TestModule {

    @Override
    protected void configure() {
        bind(AllEntitiesInSystemObservable.class).to(AllEntitiesInSystemTestObservable.class);

        install( new CommonModule());
        install( new MapModule() );
    }
}
