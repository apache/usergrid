/*
 * Created by IntelliJ IDEA.
 * User: akarasulu
 * Date: 11/22/13
 * Time: 11:44 PM
 */
package org.apache.usergrid.perftest;

import org.apache.usergrid.perftest.logging.Slf4jTypeListener;
import org.apache.usergrid.perftest.rest.*;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.inject.matcher.Matchers;
import com.google.inject.servlet.ServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import org.apache.usergrid.perfteststats.CallStats;

import java.util.HashMap;
import java.util.Map;


public class PerftestModule extends ServletModule {
    public static final String PACKAGES_KEY = "com.sun.jersey.config.property.packages";


    protected void configureServlets() {
        bindListener( Matchers.any(), new Slf4jTypeListener() );

        // Hook Jersey into Guice Servlet
        bind( GuiceContainer.class );

        // Hook Jackson into Jersey as the POJO <-> JSON mapper
        bind( JacksonJsonProvider.class ).asEagerSingleton();

        bind( CallStats.class );
        bind( PerftestRunner.class );
        bind( TestModuleLoader.class );
        bind( PerftestResetResource.class ).asEagerSingleton();
        bind( PerftestStopResource.class ).asEagerSingleton();
        bind( PerftestStartResource.class ).asEagerSingleton();
        bind( PerftestStatsResource.class ).asEagerSingleton();
        bind( PerftestStatusResource.class ).asEagerSingleton();

        Map<String, String> params = new HashMap<String, String>();
        params.put( PACKAGES_KEY, getClass().getPackage().toString() );
        serve("/*").with( GuiceContainer.class, params );
    }
}
