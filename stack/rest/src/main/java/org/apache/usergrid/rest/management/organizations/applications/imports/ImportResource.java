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


import com.sun.jersey.api.json.JSONWithPadding;
import org.apache.usergrid.exception.NotImplementedException;
import org.apache.usergrid.management.importer.ImportService;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.security.annotations.RequireOrganizationAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.UUID;


@Component("org.apache.usergrid.rest.management.organizations.applications.imports.ImportResource")
@Scope("prototype")
@Produces({
    MediaType.APPLICATION_JSON,
    "application/javascript",
    "application/x-javascript",
    "text/ecmascript",
    "application/ecmascript",
    "text/jscript"
})
public class ImportResource extends AbstractContextResource {

    private static final Logger logger = LoggerFactory.getLogger( ImportResource.class );

    private UUID importId;

    @Autowired
    ImportService importService;


    public ImportResource() {
    }


    public ImportResource init( final UUID importId ) {
        this.importId = importId;
        return this;
    }


    @GET
    public JSONWithPadding get() throws Exception {

        logger.info( "ImportResource.get" );

        ApiResponse response = createApiResponse();
        response.setAction( "Get Import" );

        EntityManager emMgmtApp = emf.getEntityManager( emf.getManagementAppId() );
        Entity importEntity = emMgmtApp.get(importId);

        response.setEntities( Collections.singletonList( importEntity ));

        return new JSONWithPadding( response );
    }


    @DELETE
    @RequireOrganizationAccess
    public JSONWithPadding executeDelete( @Context UriInfo ui,
        @QueryParam("callback") @DefaultValue("callback") String callback ) throws Exception {

        throw new NotImplementedException( "Organization delete is not allowed yet" );
    }
}
