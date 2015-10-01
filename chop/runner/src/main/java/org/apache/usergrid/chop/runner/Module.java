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
package org.apache.usergrid.chop.runner;


import java.util.HashMap;
import java.util.Map;

import org.apache.usergrid.chop.api.CoordinatorFig;
import org.apache.usergrid.chop.client.ChopClientModule;

import org.apache.usergrid.chop.runner.rest.StatusResource;
import org.apache.usergrid.chop.runner.rest.ResetResource;
import org.apache.usergrid.chop.runner.rest.StatsResource;
import org.apache.usergrid.chop.runner.rest.StopResource;
import org.apache.usergrid.chop.runner.rest.StartResource;
import org.apache.usergrid.chop.spi.RunManager;
import org.apache.usergrid.chop.spi.RunnerRegistry;
import org.safehaus.guicyfig.GuicyFigModule;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.inject.servlet.ServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;


public class Module extends ServletModule {
    public static final String PACKAGES_KEY = "com.sun.jersey.config.property.packages";


    protected void configureServlets() {
        //noinspection unchecked
        install( new GuicyFigModule( ServletFig.class, CoordinatorFig.class ) );
        install( new ChopClientModule() );

        // Hook Jersey into Guice Servlet
        bind( GuiceContainer.class );

        // Hook Jackson into Jersey as the POJO <-> JSON mapper
        bind( JacksonJsonProvider.class ).asEagerSingleton();

        bind( IController.class ).to( Controller.class );
        bind( RunnerRegistry.class ).to( RunnerRegistryImpl.class );
        bind( RunManager.class ).to( RunManagerImpl.class );

        bind( ResetResource.class );
        bind( StopResource.class );
        bind( StartResource.class );
        bind( StatsResource.class );
        bind( StatusResource.class );

        Map<String, String> params = new HashMap<String, String>();
        params.put( PACKAGES_KEY, getClass().getPackage().toString() );
        serve( "/*" ).with( GuiceContainer.class, params );
    }
}
