/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.usergrid.chop.runner.rest;


import java.util.Date;

import javax.annotation.Nullable;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.usergrid.chop.api.Runner;
import org.apache.usergrid.chop.api.StatsSnapshot;
import org.apache.usergrid.chop.runner.IController;
import org.safehaus.jettyjam.utils.TestMode;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/** ... */
@Singleton
@Produces( MediaType.APPLICATION_JSON )
@Path( Runner.STATS_GET )
public class StatsResource extends TestableResource {
    private final IController controller;


    @Inject
    public StatsResource( IController controller ) {
        super( Runner.STATS_GET );
        this.controller = controller;
    }


    @GET
    public Response getCallStatsSnapshot( @Nullable @QueryParam( TestMode.TEST_MODE_PROPERTY ) String testMode ) {
        if ( inTestMode( testMode ) ) {
            return Response.ok( new StatsSnapshot( 5L, 333L, 111L, 222L, true, new Date().getTime(), 50 ) ).build();
        }

        return Response.ok( controller.getCurrentChopStats() ).build();
    }
}
