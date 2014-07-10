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
package org.apache.usergrid.chop.webapp.coordinator.rest;


import java.io.IOException;
import java.util.ArrayList;

import javax.annotation.Nullable;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.safehaus.jettyjam.utils.TestMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.chop.api.CoordinatorFig;
import org.apache.usergrid.chop.api.RestParams;
import org.apache.usergrid.chop.stack.CoordinatedStack;
import org.apache.usergrid.chop.stack.ICoordinatedCluster;
import org.apache.usergrid.chop.stack.SetupStackState;
import org.apache.usergrid.chop.webapp.coordinator.StackCoordinator;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * A resource that uses path parameters to expose properties for a stack build. This will be
 * used when starting runners to have them pick up properties needed to hit stack clusters.
 *
 *    https://${endpoint}:${port}/properties/${user}/${groupId}/${artifactId}/${version}/${commitId}
 */
@Singleton
@Produces( MediaType.APPLICATION_JSON )
@Path( CoordinatorFig.PROPERTIES_PATH_DEFAULT )
public class PropertiesResource extends TestableResource implements RestParams {

    private static final Logger LOG = LoggerFactory.getLogger( PropertiesResource.class );

    @Inject
    StackCoordinator coordinator;


    public PropertiesResource() {
        super( CoordinatorFig.PROPERTIES_PATH_DEFAULT );
    }


    @GET
    @Path(
            "{" +
            USERNAME + "}/{" +
            MODULE_GROUPID + "}/{" +
            MODULE_ARTIFACTID + "}/{" +
            MODULE_VERSION + "}/{" +
            COMMIT_ID + "}"
    )
    public Response getProperties(
            @PathParam( USERNAME ) String user,
            @PathParam( MODULE_GROUPID ) String groupId,
            @PathParam( MODULE_ARTIFACTID ) String artifactId,
            @PathParam( MODULE_VERSION ) String version,
            @PathParam( COMMIT_ID ) String commitId,
            @Nullable @QueryParam( TestMode.TEST_MODE_PROPERTY ) String testMode
                                      ) throws IOException {
        if( inTestMode( testMode ) ) {
            LOG.info( "Calling /properties in test mode..." );
            return Response.ok( new ArrayList<ICoordinatedCluster>() ).build();
        }
        LOG.info( "Calling /properties..." );

        // Get the stack matching these parameters
        CoordinatedStack stack = coordinator.findCoordinatedStack( commitId, artifactId, groupId, version, user );
        if( stack == null ) {
            LOG.info( "No stack could be found that matches given parameters" );
            return Response.ok().entity( new ArrayList<ICoordinatedCluster>() ).build();
        }
        else if( stack.getSetupState() != SetupStackState.SetUp ) {
            LOG.info( "Stack's setup state is: " + stack.getSetupState() );
            return Response.ok().entity( new ArrayList<ICoordinatedCluster>() ).build();
        }

        return Response.ok( stack.getClusters() ).build();
    }
}
