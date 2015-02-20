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


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.importer.ImportService;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.entities.FileImport;
import org.apache.usergrid.persistence.entities.Import;
import org.apache.usergrid.persistence.exceptions.EntityNotFoundException;
import org.apache.usergrid.persistence.queue.impl.UsergridAwsCredentials;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.RootResource;
import org.apache.usergrid.rest.security.annotations.RequireOrganizationAccess;

import com.sun.jersey.api.json.JSONWithPadding;


@Component("org.apache.usergrid.rest.management.organizations.applications.imports.FileIncludesResource")
@Scope("prototype")
@Produces(MediaType.APPLICATION_JSON)
public class FileIncludesResource extends AbstractContextResource {


    @Autowired
    protected ImportService importService;

    private ApplicationInfo application;
    private UUID importId;

    /**
     * Override our service manager factory so that we get entities from the root management app
     */
    public FileIncludesResource() {
        //override the services management app

    }


    public FileIncludesResource init(  final ApplicationInfo application, final UUID importId){
        this.application = application;
        this.importId = importId;
        return this;
    }



    @GET
    public JSONWithPadding getFileIncludes( @Context UriInfo ui, @QueryParam( "ql" ) String query, @QueryParam( "cursor" ) String cursor )
          throws Exception {


          final Results importResults = importService.getFileImports( application.getId(), importId, query, cursor );

          if(importResults == null){
              throw new EntityNotFoundException( "could not load import results" );
          }

          ApiResponse response = createApiResponse();


          response.setAction( "get" );
          response.setApplication( emf.getEntityManager( application.getId() ).getApplication()  );
          response.setParams( ui.getQueryParameters() );


          response.withResults( importResults );

          return new JSONWithPadding( response );

      }

    @GET
    @Path( RootResource.ENTITY_ID_PATH )
    public JSONWithPadding getFileIncludeById( @Context UriInfo ui, @PathParam( "entityId" ) PathSegment entityId )
        throws Exception {

        final UUID fileIncludeId = UUID.fromString( entityId.getPath() );
        final FileImport importEntity = importService.getFileImport( application.getId(), importId, fileIncludeId );

        if(importEntity == null){
            throw new EntityNotFoundException( "could not find import with uuid " + importId );
        }

        ApiResponse response = createApiResponse();


        response.setAction( "get" );
        response.setApplication( emf.getEntityManager( application.getId() ).getApplication()  );
        response.setParams( ui.getQueryParameters() );


        response.setEntities( Collections.<Entity>singletonList( importEntity ) );

        return new JSONWithPadding( response );

    }





    @Path( RootResource.ENTITY_ID_PATH + "/errors" )
    public FileErrorsResource getIncludes( @Context UriInfo ui, @PathParam( "entityId" ) PathSegment entityId )
        throws Exception {

        final UUID fileImportId = UUID.fromString( entityId.getPath() );
        return getSubResource( FileErrorsResource.class ).init( application, importId,fileImportId  );

    }




}
