/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.webapp;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.inject.servlet.ServletModule;
import com.netflix.config.ConfigurationManager;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import org.apache.shiro.guice.web.ShiroWebModule;
import org.apache.usergrid.chop.api.Project;
import org.apache.usergrid.chop.api.store.amazon.AmazonModule;
import org.apache.usergrid.chop.webapp.coordinator.RunnerCoordinator;
import org.apache.usergrid.chop.webapp.coordinator.rest.*;
import org.apache.usergrid.chop.webapp.elasticsearch.ElasticSearchClient;
import org.apache.usergrid.chop.webapp.elasticsearch.ElasticSearchFig;
import org.apache.usergrid.chop.webapp.elasticsearch.IElasticSearchClient;
import org.apache.usergrid.chop.webapp.view.util.VaadinServlet;
import org.safehaus.guicyfig.GuicyFigModule;

import com.google.inject.Singleton;
import org.apache.usergrid.chop.webapp.coordinator.rest.AuthResource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@SuppressWarnings( "unchecked" )
@Singleton
public class ChopUiModule extends ServletModule {

    public static final String PACKAGES_KEY = "com.sun.jersey.config.property.packages";

    static {
        try {
            ConfigurationManager.loadCascadedPropertiesFromResources( "chop-ui" );
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Could not load configuration file", e );
        }
    }

    protected void configureServlets() {
        install( new GuicyFigModule( ChopUiFig.class, Project.class, RestFig.class, ElasticSearchFig.class ) );
        install( new AmazonModule() );

        // Hook Jersey into Guice Servlet
        bind( GuiceContainer.class );

        bind( IElasticSearchClient.class ).to( ElasticSearchClient.class );

        // Hook Jackson into Jersey as the POJO <-> JSON mapper
        bind( JacksonJsonProvider.class ).asEagerSingleton();

        bind( UploadResource.class ).asEagerSingleton();
        bind( RunManagerResource.class ).asEagerSingleton();
        bind( TestGetResource.class ).asEagerSingleton();
        bind( AuthResource.class ).asEagerSingleton();
        bind( PropertiesResource.class ).asEagerSingleton();
        bind( RunnerCoordinator.class ).asEagerSingleton();

        ShiroWebModule.bindGuiceFilter( binder() );

        // This should be before "/*" otherwise the vaadin servlet will not work
        serve( "/VAADIN*" ).with( VaadinServlet.class );

        Map<String, String> params = new HashMap<String, String>();
        params.put( PACKAGES_KEY, getClass().getPackage().toString() );
        serve( "/*" ).with( GuiceContainer.class, params );
    }
}
