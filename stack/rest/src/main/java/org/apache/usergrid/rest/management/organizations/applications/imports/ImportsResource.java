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
import javax.xml.ws.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.ObjectUtils;

import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.importer.ImportService;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.entities.Import;
import org.apache.usergrid.persistence.exceptions.EntityNotFoundException;
import org.apache.usergrid.persistence.queue.impl.UsergridAwsCredentials;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.RootResource;
import org.apache.usergrid.rest.applications.ServiceResource;
import org.apache.usergrid.rest.security.annotations.RequireOrganizationAccess;
import org.apache.usergrid.rest.utils.JSONPUtils;

import com.sun.jersey.api.json.JSONWithPadding;


@Component( "org.apache.usergrid.rest.management.organizations.applications.imports.ImportsResource" )
@Scope( "prototype" )
@Produces( MediaType.APPLICATION_JSON )
public class ImportsResource extends AbstractContextResource {


    @Autowired
    protected ImportService importService;

    private OrganizationInfo organization;
    private ApplicationInfo application;


    /**
     * Override our service manager factory so that we get entities from the root management app
     */
    public ImportsResource() {
        //override the services management app

    }


    public ImportsResource init( final OrganizationInfo organization, final ApplicationInfo application ) {
        this.organization = organization;
        this.application = application;
        return this;
    }


    @POST
    @RequireOrganizationAccess
    @Consumes( MediaType.APPLICATION_JSON )
    public JSONWithPadding executePost( @Context UriInfo ui, String body,
                                        @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
        throws Exception {


        ApiResponse response = createApiResponse();


        response.setAction( "post" );
        response.setApplication( emf.getEntityManager( application.getId() ).getApplication() );
        response.setParams( ui.getQueryParameters() );

        final Map<String, Object> json = ( Map<String, Object> ) readJsonToObject( body );

        Map<String, Object> properties;
        Map<String, Object> storage_info;
        // UsergridAwsCredentialsProvider uacp = new UsergridAwsCredentialsProvider();

        //             try {
        //checkJsonExportProperties(json);


            if ( ( properties = ( Map<String, Object> ) json.get( "properties" ) ) == null ) {
                throw new NullArgumentException( "Could not find 'properties'" );
            }
            storage_info = ( Map<String, Object> ) properties.get( "storage_info" );
            String storage_provider = ( String ) properties.get( "storage_provider" );
            if ( storage_provider == null ) {
                throw new NullArgumentException( "Could not find field 'storage_provider'" );
            }
            if ( storage_info == null ) {
                throw new NullArgumentException( "Could not find field 'storage_info'" );
            }

            String bucketName = ( String ) storage_info.get( "bucket_location" );


            String accessId = ( String ) storage_info.get( "s3_access_id" );
            String secretKey = ( String ) storage_info.get( "s3_key" );

            if ( bucketName == null ) {
                throw new NullArgumentException( "Could not find field 'bucketName'" );
            }
            if ( accessId == null ) {
                throw new NullArgumentException( "Could not find field 's3_access_id'" );
            }
            if ( secretKey == null ) {

                throw new NullArgumentException( "Could not find field 's3_key'" );
            }



        json.put( "organizationId", organization.getUuid() );
        json.put( "applicationId", application.getId() );

        Import importEntity = importService.schedule( application.getId(), json );

        response.setEntities( Collections.<Entity>singletonList( importEntity ) );

        return new JSONWithPadding( response, callback );
    }


    @GET
    public JSONWithPadding getImports( @Context UriInfo ui, @QueryParam( "ql" ) String query,  @QueryParam( "cursor" ) String cursor ) throws Exception {


        final Results importResults = importService.getImports( application.getId(), query, cursor );

        if ( importResults == null ) {
            throw new EntityNotFoundException( "could not load import results" );
        }

        ApiResponse response = createApiResponse();


        response.setAction( "get" );
        response.setApplication( emf.getEntityManager( application.getId() ).getApplication() );
        response.setParams( ui.getQueryParameters() );


        response.withResults( importResults );

        return new JSONWithPadding( response );
    }


    @GET
    @Path( RootResource.ENTITY_ID_PATH )
    public JSONWithPadding getImportById( @Context UriInfo ui, @PathParam( "entityId" ) PathSegment entityId )
        throws Exception {

        final UUID importId = UUID.fromString( entityId.getPath() );
        final Import importEntity = importService.getImport( application.getId(), importId );

        if ( importEntity == null ) {
            throw new EntityNotFoundException( "could not find import with uuid " + importId );
        }

        ApiResponse response = createApiResponse();


        response.setAction( "get" );
        response.setApplication( emf.getEntityManager( application.getId() ).getApplication() );
        response.setParams( ui.getQueryParameters() );


        response.setEntities( Collections.<Entity>singletonList( importEntity ) );

        return new JSONWithPadding( response );
    }


    @Path( RootResource.ENTITY_ID_PATH + "/files" )
    public FileIncludesResource getIncludes( @Context UriInfo ui, @PathParam( "entityId" ) PathSegment entityId )
        throws Exception {
        final UUID importId = UUID.fromString( entityId.getPath() );
        return getSubResource( FileIncludesResource.class ).init( application, importId );
    }
}
