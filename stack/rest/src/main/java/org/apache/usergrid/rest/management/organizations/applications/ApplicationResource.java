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
package org.apache.usergrid.rest.management.organizations.applications;


import com.google.common.base.Preconditions;
import com.sun.jersey.api.json.JSONWithPadding;
import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.amber.oauth2.common.message.OAuthResponse;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;

import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.export.ExportService;
import org.apache.usergrid.persistence.queue.impl.UsergridAwsCredentials;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.applications.ServiceResource;
import org.apache.usergrid.rest.management.organizations.applications.imports.ImportsResource;
import org.apache.usergrid.rest.security.annotations.RequireOrganizationAccess;
import org.apache.usergrid.rest.utils.JSONPUtils;
import org.apache.usergrid.security.oauth.ClientCredentialsInfo;
import org.apache.usergrid.security.providers.SignInAsProvider;
import org.apache.usergrid.security.providers.SignInProviderFactory;
import org.apache.usergrid.services.ServiceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static javax.servlet.http.HttpServletResponse.SC_ACCEPTED;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.core.util.Health;


@Component("org.apache.usergrid.rest.management.organizations.applications.ApplicationResource")
@Scope("prototype")
@Produces({
    MediaType.APPLICATION_JSON,
    "application/javascript",
    "application/x-javascript",
    "text/ecmascript",
    "application/ecmascript",
    "text/jscript"
})
public class ApplicationResource extends AbstractContextResource {

    @Autowired
    protected ExportService exportService;

    OrganizationInfo organization;
    UUID applicationId;
    ApplicationInfo application;

    @Autowired
    private SignInProviderFactory signInProviderFactory;


    public ApplicationResource() {
    }


    public ApplicationResource init( OrganizationInfo organization, UUID applicationId ) {
        this.organization = organization;
        this.applicationId = applicationId;
        return this;
    }


    public ApplicationResource init( OrganizationInfo organization, ApplicationInfo application ) {
        this.organization = organization;
        applicationId = application.getId();
        this.application = application;
        return this;
    }


    @RequireOrganizationAccess
    @DELETE
    public JSONWithPadding deleteApplicationFromOrganizationByApplicationId(
            @Context UriInfo ui, @QueryParam("callback") @DefaultValue("callback") String callback )
        throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "delete application from organization" );

        management.deleteOrganizationApplication( organization.getUuid(), applicationId );

        return new JSONWithPadding( response, callback );
    }


    @RequireOrganizationAccess
    @GET
    public JSONWithPadding getApplication(
            @Context UriInfo ui, @QueryParam("callback") @DefaultValue("callback") String callback )
        throws Exception {

        ApiResponse response = createApiResponse();
        ServiceManager sm = smf.getServiceManager( applicationId );
        response.setAction( "get" );
        response.setApplication( sm.getApplication() );
        response.setParams( ui.getQueryParameters() );
        response.setResults( management.getApplicationMetadata( applicationId ) );
        return new JSONWithPadding( response, callback );
    }


    @RequireOrganizationAccess
    @GET
    @Path("credentials")
    public JSONWithPadding getCredentials(
            @Context UriInfo ui, @QueryParam("callback") @DefaultValue("callback") String callback )
        throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "get application client credentials" );

        ClientCredentialsInfo credentials =
                new ClientCredentialsInfo( management.getClientIdForApplication( applicationId ),
                        management.getClientSecretForApplication( applicationId ) );

        response.setCredentials( credentials );
        return new JSONWithPadding( response, callback );
    }


    @RequireOrganizationAccess
    @POST
    @Path("credentials")
    public JSONWithPadding generateCredentials( @Context UriInfo ui,
            @QueryParam("callback") @DefaultValue("callback") String callback )
        throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "generate application client credentials" );

        ClientCredentialsInfo credentials =
                new ClientCredentialsInfo( management.getClientIdForApplication( applicationId ),
                        management.newClientSecretForApplication( applicationId ) );

        response.setCredentials( credentials );
        return new JSONWithPadding( response, callback );
    }


    @POST
    @Path("sia-provider")
    @Consumes(APPLICATION_JSON)
    @RequireOrganizationAccess
    public JSONWithPadding configureProvider(
            @Context UriInfo ui,
            @QueryParam("provider_key") String siaProvider,
            Map<String, Object> json,
            @QueryParam("callback")
            @DefaultValue("") String callback )
        throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "post signin provider configuration" );

        Preconditions.checkArgument( siaProvider != null, "Sign in provider required" );

        SignInAsProvider signInAsProvider = null;
        if ( StringUtils.equalsIgnoreCase( siaProvider, "facebook" ) ) {
            signInAsProvider = signInProviderFactory.facebook(
                    smf.getServiceManager( applicationId ).getApplication() );
        }
        else if ( StringUtils.equalsIgnoreCase( siaProvider, "pingident" ) ) {
            signInAsProvider = signInProviderFactory.pingident(
                    smf.getServiceManager( applicationId ).getApplication() );
        }
        else if ( StringUtils.equalsIgnoreCase( siaProvider, "foursquare" ) ) {
            signInAsProvider = signInProviderFactory.foursquare(
                    smf.getServiceManager( applicationId ).getApplication() );
        }

        Preconditions.checkArgument( signInAsProvider != null,
                "No signin provider found by that name: " + siaProvider );

        signInAsProvider.saveToConfiguration( json );

        return new JSONWithPadding( response, callback );
    }

    @POST
    @Path("export")
    @Consumes(APPLICATION_JSON)
    @RequireOrganizationAccess
    public Response exportPostJson( @Context UriInfo ui,Map<String, Object> json,
                                    @QueryParam("callback") @DefaultValue("") String callback )
            throws OAuthSystemException {

        UsergridAwsCredentials uac = new UsergridAwsCredentials();

        UUID jobUUID = null;
        Map<String, String> uuidRet = new HashMap<String, String>();

        Map<String,Object> properties;
        Map<String, Object> storage_info;

        try {
            if((properties = ( Map<String, Object> )  json.get( "properties" )) == null){
                throw new NullArgumentException("Could not find 'properties'");
            }
            storage_info = ( Map<String, Object> ) properties.get( "storage_info" );
            String storage_provider = ( String ) properties.get( "storage_provider" );
            if(storage_provider == null) {
                throw new NullArgumentException( "Could not find field 'storage_provider'" );
            }
            if(storage_info == null) {
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

            json.put("organizationId", organization.getUuid());
            json.put( "applicationId",applicationId);

            jobUUID = exportService.schedule( json );
            uuidRet.put( "Export Entity", jobUUID.toString() );
        }
        catch ( NullArgumentException e ) {
            return Response.status( SC_BAD_REQUEST )
                .type( JSONPUtils.jsonMediaType( callback ) )
                .entity( ServiceResource.wrapWithCallback( e.getMessage(), callback ) ).build();
        }
        catch ( Exception e ) {
            // TODO: throw descriptive error message and or include on in the response
            // TODO: fix below, it doesn't work if there is an exception.
            // Make it look like the OauthResponse.
            return Response.status( SC_INTERNAL_SERVER_ERROR )
                .type( JSONPUtils.jsonMediaType( callback ) )
                .entity( ServiceResource.wrapWithCallback( e.getMessage(), callback ) ).build();
        }

        return Response.status( SC_ACCEPTED ).entity( uuidRet ).build();
    }

    @POST
    @Path("collection/{collection_name}/export")
    @Consumes(APPLICATION_JSON)
    @RequireOrganizationAccess
    public Response exportPostJson( @Context UriInfo ui,
            @PathParam( "collection_name" ) String collection_name ,Map<String, Object> json,
            @QueryParam("callback") @DefaultValue("") String callback )
            throws OAuthSystemException {

        UsergridAwsCredentials uac = new UsergridAwsCredentials();
        UUID jobUUID = null;
        String colExport = collection_name;
        Map<String, String> uuidRet = new HashMap<String, String>();

        Map<String,Object> properties;
        Map<String, Object> storage_info;

        try {
            //checkJsonExportProperties(json);
            if((properties = ( Map<String, Object> )  json.get( "properties" )) == null){
                throw new NullArgumentException("Could not find 'properties'");
            }
            storage_info = ( Map<String, Object> ) properties.get( "storage_info" );
            String storage_provider = ( String ) properties.get( "storage_provider" );
            if(storage_provider == null) {
                throw new NullArgumentException( "Could not find field 'storage_provider'" );
            }
            if(storage_info == null) {
                throw new NullArgumentException( "Could not find field 'storage_info'" );
            }

            String bucketName = ( String ) storage_info.get( "bucket_location" );
            String accessId = ( String ) storage_info.get( "s3_access_id" );
            String secretKey = ( String ) storage_info.get( "s3_key" );

            if ( accessId == null ) {
                throw new NullArgumentException( "Could not find field 's3_access_id'" );
            }
            if ( secretKey == null ) {
                throw new NullArgumentException( "Could not find field 's3_key'" );
            }

            if(bucketName == null) {
                throw new NullArgumentException( "Could not find field 'bucketName'" );
            }

            json.put( "organizationId",organization.getUuid() );
            json.put( "applicationId", applicationId);
            json.put( "collectionName", colExport);

            jobUUID = exportService.schedule( json );
            uuidRet.put( "Export Entity", jobUUID.toString() );
        }
        catch ( NullArgumentException e ) {
            return Response.status( SC_BAD_REQUEST )
                .type( JSONPUtils.jsonMediaType( callback ) )
                .entity( ServiceResource.wrapWithCallback( e.getMessage(), callback ) )
                .build();
        }
        catch ( Exception e ) {

            // TODO: throw descriptive error message and or include on in the response
            // TODO: fix below, it doesn't work if there is an exception.
            // Make it look like the OauthResponse.

            OAuthResponse errorMsg = OAuthResponse.errorResponse( SC_INTERNAL_SERVER_ERROR )
                .setErrorDescription( e.getMessage() )
                .buildJSONMessage();

            return Response.status( errorMsg.getResponseStatus() )
                .type( JSONPUtils.jsonMediaType( callback ) )
                .entity( ServiceResource.wrapWithCallback( errorMsg.getBody(), callback ) )
                .build();
        }

        return Response.status( SC_ACCEPTED ).entity( uuidRet ).build();
    }


    @Path( "imports" )
    @RequireOrganizationAccess
    public ImportsResource importGetJson( @Context UriInfo ui,
                                          @QueryParam( "callback" ) @DefaultValue( "" ) String callback )
        throws Exception {


        return getSubResource( ImportsResource.class ).init( organization, application );
    }

    @GET
    @Path("/status")
    public Response getStatus() {

        Map<String, Object> statusMap = new HashMap<String, Object>();

        EntityManager em = emf.getEntityManager( applicationId );
        if ( !emf.getIndexHealth().equals( Health.RED ) ) {
            statusMap.put("message", "Index Health Status RED for application " + applicationId );
            return Response.status( SC_INTERNAL_SERVER_ERROR ).entity( statusMap ).build();
        }

        try {
            if ( em.getApplication() == null ) {
                statusMap.put("message", "Application " + applicationId + " not found");
                return Response.status( SC_NOT_FOUND ).entity( statusMap ).build();
            }

        } catch (Exception ex) {
            statusMap.put("message", "Error looking up application " + applicationId );
            return Response.status( SC_INTERNAL_SERVER_ERROR ).entity( statusMap ).build();
        }

        return Response.status( SC_OK ).entity( null ).build();
    }
}
