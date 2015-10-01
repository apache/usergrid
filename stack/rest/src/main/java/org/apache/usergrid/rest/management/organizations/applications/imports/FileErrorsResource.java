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


import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.importer.ImportService;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.entities.FailedImportEntity;
import org.apache.usergrid.persistence.exceptions.EntityNotFoundException;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.RootResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.UUID;


@Component("org.apache.usergrid.rest.management.organizations.applications.imports.FileErrorsResource")
@Scope("prototype")
@Produces(MediaType.APPLICATION_JSON)
public class FileErrorsResource extends AbstractContextResource {


    @Autowired
    protected ImportService importService;

    private ApplicationInfo application;
    private UUID importId;
    private UUID importFileId;

    /**
     * Override our service manager factory so that we get entities from the root management app
     */
    public FileErrorsResource() {
        //override the services management app

    }


    public FileErrorsResource init( final ApplicationInfo application, final UUID importId, final UUID importFileId){
        this.application = application;
        this.importId = importId;
        this.importFileId = importFileId;
        return this;
    }



    @GET
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse getFileIncludes( @Context UriInfo ui, @QueryParam( "ql" ) String query, @QueryParam( "cursor" ) String cursor )
          throws Exception {


          final Results importResults = importService.getFailedImportEntities( application.getId(), importId,
              importFileId, query,  cursor );

          if(importResults == null){
              throw new EntityNotFoundException( "could not load import results" );
          }

          ApiResponse response = createApiResponse();


          response.setAction( "get" );
          response.setApplication( emf.getEntityManager( application.getId() ).getApplication()  );
          response.setParams( ui.getQueryParameters() );


          response.withResults( importResults );

          return response;

      }

    @GET
    @Path( RootResource.ENTITY_ID_PATH )
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse getFileIncludeById( @Context UriInfo ui, @PathParam( "entityId" ) PathSegment entityId )
        throws Exception {

        final UUID failedEntity = UUID.fromString( entityId.getPath() );
        final FailedImportEntity
            importEntity = importService.getFailedImportEntity( application.getId(), importId, importFileId,
            failedEntity );

        if(importEntity == null){
            throw new EntityNotFoundException( "could not find import with uuid " + importId );
        }

        ApiResponse response = createApiResponse();


        response.setAction( "get" );
        response.setApplication( emf.getEntityManager( application.getId() ).getApplication()  );
        response.setParams( ui.getQueryParameters() );


        response.setEntities( Collections.<Entity>singletonList( importEntity ) );

        return response;

    }






}
