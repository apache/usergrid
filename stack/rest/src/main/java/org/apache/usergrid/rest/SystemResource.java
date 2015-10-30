/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.rest;


import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.usergrid.rest.system.ApplicationsResource;
import org.apache.usergrid.rest.system.DatabaseResource;
import org.apache.usergrid.rest.system.QueueSystemResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import org.apache.usergrid.rest.security.annotations.RequireSystemAccess;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;



@Path( "/system" )
@Component
@Scope( "singleton" )
@Produces( {
    MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
    "application/ecmascript", "text/jscript"
} )
public class SystemResource extends AbstractContextResource {

    private static final Logger logger = LoggerFactory.getLogger( SystemResource.class );


    public SystemResource() {
        logger.info( "SystemResource initialized" );
    }


    @RequireSystemAccess
    @GET

    @Path( "superuser/setup" )
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse getSetupSuperuser( @Context UriInfo ui,
           @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
        throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "superuser setup" );

        logger.info( "Setting up Superuser" );

        try {
            management.provisionSuperuser();
        }
        catch ( Exception e ) {
            logger.error( "Unable to complete superuser setup", e );
        }

        response.setSuccess();

        return response;
    }



    @Path( "migrate" )
    public MigrateResource migrate() {
        return getSubResource( MigrateResource.class );
    }


    @Path( "index" )
    public IndexResource index() { return getSubResource( IndexResource.class ); }


    @Path( "database" )
    public DatabaseResource database() {
        return getSubResource( DatabaseResource.class );
    }

    @Path( "queue" )
    public QueueSystemResource queue() {
        return getSubResource( QueueSystemResource.class );
    }

    @Path( "applications" )
    public ApplicationsResource applications() {
        return getSubResource( ApplicationsResource.class );
    }
}
