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
package org.apache.usergrid.rest.system;


import javax.ws.rs.DefaultValue;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import org.apache.usergrid.rest.security.annotations.RequireSystemAccess;


@Component
@Scope( "singleton" )
@Produces( {
    MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
    "application/ecmascript", "text/jscript"
} )
public class DatabaseResource extends AbstractContextResource {

    private static final Logger logger = LoggerFactory.getLogger( DatabaseResource.class );


    public DatabaseResource() {
        logger.info( "DatabaseResource initialized" );
    }


    @RequireSystemAccess
    @PUT
    @JSONP
    @Path( "setup" )
    public ApiResponse runDatabaseSetup( @Context UriInfo ui,
        @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
        throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "cassandra setup" );

        logger.info( "Setting up Cassandra" );


        emf.setup();


        response.setSuccess();

        return response;
    }


    @RequireSystemAccess
    @PUT
    @JSONP
    @Path( "bootstrap" )
    public ApiResponse runSystemSetup( @Context UriInfo ui,
        @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
        throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "cassandra setup" );

        logger.info( "Setting up Cassandra" );


        emf.boostrap();
        management.setup();

        response.setSuccess();

        return response;
    }
}

