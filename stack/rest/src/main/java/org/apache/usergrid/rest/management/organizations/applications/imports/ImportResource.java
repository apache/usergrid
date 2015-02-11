/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.rest.management.organizations.applications.imports;


import java.util.UUID;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import org.apache.shiro.authz.UnauthorizedException;

import org.apache.usergrid.exception.NotImplementedException;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.RootResource;
import org.apache.usergrid.rest.applications.ApplicationResource;
import org.apache.usergrid.rest.exceptions.NoOpException;
import org.apache.usergrid.rest.exceptions.OrganizationApplicationNotFoundException;
import org.apache.usergrid.rest.security.annotations.RequireOrganizationAccess;
import org.apache.usergrid.rest.utils.PathingUtils;
import org.apache.usergrid.security.shiro.utils.SubjectUtils;

import com.google.common.collect.BiMap;
import com.sun.jersey.api.json.JSONWithPadding;


@Component("org.apache.usergrid.rest.management.organizations.applications.imports.ImportResource")
@Scope("prototype")
@Produces({
        MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
        "application/ecmascript", "text/jscript"
})
public class ImportResource extends AbstractContextResource {

    private static final Logger logger = LoggerFactory.getLogger( ImportResource.class );

    private UUID importId;

    public ImportResource() {

    }


    public ImportResource init( final UUID importId ) {
       this.importId = importId;
        return this;
    }


    @GET
    public JSONWithPadding get(  )
            throws Exception {


        logger.info( "UserResource.sendPin" );

        ApiResponse response = createApiResponse();
        response.setAction( "Get Import" );



        return new JSONWithPadding( response );
    }




    @DELETE
    @RequireOrganizationAccess
    public JSONWithPadding executeDelete( @Context UriInfo ui,
                                          @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {


        throw new NotImplementedException( "Organization delete is not allowed yet" );
    }
}
