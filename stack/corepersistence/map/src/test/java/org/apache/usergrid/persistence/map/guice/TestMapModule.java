package org.apache.usergrid.persistence.map.guice;


import org.apache.usergrid.persistence.core.guice.TestModule;
import org.apache.usergrid.persistence.core.guice.CommonModule;
import org.apache.usergrid.persistence.core.rx.AllEntitiesInSystemObservable;
import org.apache.usergrid.persistence.core.rx.AllEntitiesInSystemTestObservable;
import org.apache.usergrid.persistence.core.rx.ApplicationObservable;
import org.apache.usergrid.persistence.core.rx.ApplicationsTestObservable;


public class TestMapModule extends TestModule {

    @Override
    protected void configure() {
        bind(AllEntitiesInSystemObservable.class).to(AllEntitiesInSystemTestObservable.class);
        bind(ApplicationObservable.class).to(ApplicationsTestObservable.class);

        install( new CommonModule());
        install( new MapModule() );
    }
}
