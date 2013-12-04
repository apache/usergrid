/*
 * Created by IntelliJ IDEA.
 * User: akarasulu
 * Date: 11/22/13
 * Time: 11:44 PM
 */
package org.apache.usergrid.perftest;

import org.apache.usergrid.perftest.rest.*;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.inject.servlet.ServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

import java.util.HashMap;
import java.util.Map;


public class PerftestModule extends ServletModule {
    public static final String PACKAGES_KEY = "com.sun.jersey.config.property.packages";


    protected void configureServlets() {
        // Hook Jersey into Guice Servlet
        bind( GuiceContainer.class );

        // Hook Jackson into Jersey as the POJO <-> JSON mapper
        bind( JacksonJsonProvider.class ).asEagerSingleton();

        bind( ResultsLog.class ).to( ResultsLogImpl.class );

        bind( CallStats.class );
        bind( PerftestRunner.class );
        bind( TestModuleLoader.class );
        bind( ResetResource.class ).asEagerSingleton();
        bind( StopResource.class ).asEagerSingleton();
        bind( StartResource.class ).asEagerSingleton();
        bind( StatsResource.class ).asEagerSingleton();
        bind( StatusResource.class ).asEagerSingleton();

        Map<String, String> params = new HashMap<String, String>();
        params.put( PACKAGES_KEY, getClass().getPackage().toString() );
        serve("/*").with( GuiceContainer.class, params );
    }
}
