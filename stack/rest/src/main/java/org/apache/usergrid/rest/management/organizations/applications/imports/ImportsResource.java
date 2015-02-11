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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.amber.oauth2.common.message.OAuthResponse;
import org.apache.commons.lang.StringUtils;

import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.importer.ImportService;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.entities.Import;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.persistence.queue.impl.UsergridAwsCredentials;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.RootResource;
import org.apache.usergrid.rest.applications.ServiceResource;
import org.apache.usergrid.rest.applications.users.UserResource;
import org.apache.usergrid.rest.security.annotations.RequireApplicationAccess;
import org.apache.usergrid.rest.security.annotations.RequireOrganizationAccess;
import org.apache.usergrid.rest.utils.JSONPUtils;
import org.apache.usergrid.services.ServiceAction;
import org.apache.usergrid.services.ServicePayload;

import com.amazonaws.AmazonClientException;
import com.sun.jersey.api.json.JSONWithPadding;

import static javax.servlet.http.HttpServletResponse.SC_ACCEPTED;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import static org.apache.usergrid.services.ServiceParameter.addParameter;


@Component("org.apache.usergrid.rest.management.organizations.applications.ImportsResource")
@Scope("prototype")
@Produces(MediaType.APPLICATION_JSON)
public class ImportsResource extends ServiceResource {


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


    public ImportsResource init( final OrganizationInfo organization, final ApplicationInfo application ){
        this.organization = organization;
        this.application = application;
        services = smf.getServiceManager( application.getId() );
        return this;
    }


    @POST
    @RequireOrganizationAccess
    @Consumes( MediaType.APPLICATION_JSON )
    public JSONWithPadding executePost( @Context UriInfo ui, String body,
                                        @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
        throws Exception {

        LOG.debug( "ServiceResource.executePost: body = " + body );

        ApiResponse response = createApiResponse();


        response.setAction( "post" );
        response.setApplication( services.getApplication() );
        response.setParams( ui.getQueryParameters() );

        final Map<String, Object> json = ( Map<String, Object> ) readJsonToObject( body );

        UsergridAwsCredentials uac = new UsergridAwsCredentials();

        Map<String, String> uuidRet = new HashMap<String, String>();

        Map<String, Object> properties;
        Map<String, Object> storage_info;
        // UsergridAwsCredentialsProvider uacp = new UsergridAwsCredentialsProvider();

        //             try {
        //checkJsonExportProperties(json);
        if ( ( properties = ( Map<String, Object> ) json.get( "properties" ) ) == null ) {
            throw new NullPointerException( "Could not find 'properties'" );
        }
        storage_info = ( Map<String, Object> ) properties.get( "storage_info" );
        String storage_provider = ( String ) properties.get( "storage_provider" );
        if ( storage_provider == null ) {
            throw new NullPointerException( "Could not find field 'storage_provider'" );
        }
        if ( storage_info == null ) {
            throw new NullPointerException( "Could not find field 'storage_info'" );
        }

        String bucketName = ( String ) storage_info.get( "bucket_location" );

        //check to make sure that access key and secret key are there.
//        uac.getAWSAccessKeyIdJson( storage_info );
//        uac.getAWSSecretKeyJson( storage_info );

        if ( bucketName == null ) {
            throw new NullPointerException( "Could not find field 'bucketName'" );
        }

        json.put( "organizationId", organization.getUuid() );
        json.put( "applicationId", application.getId() );

        Import importEntity = importService.schedule( json );

        response.setEntities( Collections.<Entity>singletonList( importEntity ) );
        //             }
        //             catch ( NullPointerException e ) {
        //
        //                 response.set
        //                 return Response.status( SC_BAD_REQUEST ).type( JSONPUtils.jsonMediaType( callback ) )
        //                                .entity( ServiceResource.wrapWithCallback( e.getMessage(), callback ) )
        // .build();
        //             }
        //             catch ( AmazonClientException e ) {
        //                 return Response.status( SC_BAD_REQUEST ).type( JSONPUtils.jsonMediaType( callback ) )
        //                                .entity( ServiceResource.wrapWithCallback( e.getMessage(), callback ) )
        // .build();
        //             }
        //             catch ( Exception e ) {
        //                 //TODO:throw descriptive error message and or include on in the response
        //                 //TODO:fix below, it doesn't work if there is an exception. Make it look like the
        // OauthResponse.
        //                 OAuthResponse errorMsg = OAuthResponse.errorResponse( SC_INTERNAL_SERVER_ERROR )
        // .setErrorDescription( e.getMessage() )
        //                                                       .buildJSONMessage();
        //                 return Response.status( errorMsg.getResponseStatus() ).type( JSONPUtils.jsonMediaType(
        // callback ) )
        //                                .entity( ServiceResource.wrapWithCallback( errorMsg.getBody(), callback ) )
        // .build();
        //             }

        //             return Response.status( SC_ACCEPTED ).entity( uuidRet ).build();


        return new JSONWithPadding( response, callback );
    }


    @Override
    @Path( RootResource.ENTITY_ID_PATH )
    public AbstractContextResource addIdParameter( @Context UriInfo ui, @PathParam( "entityId" ) PathSegment entityId )
        throws Exception {


        UUID itemId = UUID.fromString( entityId.getPath() );

        addParameter( getServiceParameters(), itemId );

        addMatrixParams( getServiceParameters(), ui, entityId );


        return getSubResource( ImportResource.class ).init(  itemId  );
    }




}
