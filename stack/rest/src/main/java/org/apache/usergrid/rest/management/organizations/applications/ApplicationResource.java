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


import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.google.common.base.Preconditions;
import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.amber.oauth2.common.message.OAuthResponse;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.export.ExportService;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.core.util.Health;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Properties;
import java.util.UUID;

import static javax.servlet.http.HttpServletResponse.*;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;


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

    private static final Logger logger = LoggerFactory.getLogger(ApplicationResource.class);

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
    @GET
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse getApplication(
            @Context UriInfo ui, @QueryParam("callback") @DefaultValue("callback") String callback )
        throws Exception {

        ApiResponse response = createApiResponse();
        ServiceManager sm = smf.getServiceManager( applicationId );
        response.setAction( "get" );
        response.setApplication( sm.getApplication() );
        response.setParams( ui.getQueryParameters() );
        response.setResults( management.getApplicationMetadata( applicationId ) );
        return response;
    }


    @RequireOrganizationAccess
    @GET
    @Path("credentials")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse getCredentials(
            @Context UriInfo ui, @QueryParam("callback") @DefaultValue("callback") String callback )
        throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction("get application client credentials");

        ClientCredentialsInfo credentials =
                new ClientCredentialsInfo( management.getClientIdForApplication( applicationId ),
                        management.getClientSecretForApplication( applicationId ) );

        response.setCredentials( credentials );
        return response;
    }


    @RequireOrganizationAccess
    @POST
    @Path("credentials")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse generateCredentials( @Context UriInfo ui,
            @QueryParam("callback") @DefaultValue("callback") String callback )
        throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "generate application client credentials" );

        ClientCredentialsInfo credentials =
                new ClientCredentialsInfo( management.getClientIdForApplication( applicationId ),
                        management.newClientSecretForApplication(applicationId) );

        response.setCredentials( credentials );
        return response;
    }

    @RequireOrganizationAccess
    @GET
    @JSONP
    @Path("_size")
    public ApiResponse getApplicationSize(
        @Context UriInfo ui, @QueryParam("callback") @DefaultValue("callback") String callback )
        throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "get application size for all entities" );
        long size = management.getApplicationSize(this.applicationId);
        Map<String,Object> map = new HashMap<>();
        Map<String,Object> innerMap = new HashMap<>();
        Map<String,Object> sumMap = new HashMap<>();
        innerMap.put("application",size);
        sumMap.put("size",innerMap);
        map.put("aggregation", sumMap);
        response.setMetadata(map);
        return response;
    }

    @RequireOrganizationAccess
    @GET
    @JSONP
    @Path("{collection_name}/_size")
    public ApiResponse getCollectionSize(
        @Context UriInfo ui,
        @PathParam( "collection_name" ) String collection_name,
        @QueryParam("callback") @DefaultValue("callback") String callback )
        throws Exception {
        ApiResponse response = createApiResponse();
        response.setAction("get collection size for all entities");
        long size = management.getCollectionSize(this.applicationId, collection_name);
        Map<String,Object> map = new HashMap<>();
        Map<String,Object> sumMap = new HashMap<>();
        Map<String,Object> innerMap = new HashMap<>();
        innerMap.put(collection_name,size);
        sumMap.put("size",innerMap);
        map.put("aggregation",sumMap);
        response.setMetadata(map);
        return response;
    }

    @RequireOrganizationAccess
    @GET
    @JSONP
    @Path("collections/_size")
    public ApiResponse getEachCollectionSize(
        @Context UriInfo ui,
        @QueryParam("callback") @DefaultValue("callback") String callback )
        throws Exception {
        ApiResponse response = createApiResponse();
        response.setAction("get collection size for all entities");
        Map<String,Long> sizes = management.getEachCollectionSize(this.applicationId);
        Map<String,Object> map = new HashMap<>();
        Map<String,Object> sumMap = new HashMap<>();
        sumMap.put("size",sizes);
        map.put("aggregation",sumMap);
        response.setMetadata(map);
        return response;
    }

    @POST
    @Path("sia-provider")
    @Consumes(APPLICATION_JSON)
    @RequireOrganizationAccess
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse configureProvider(
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

        return response;
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



    /**
     * Put on application URL will restore application if it was deleted.
     */
    @PUT
    @RequireOrganizationAccess
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse executePut(  @Context UriInfo ui, String body,
        @QueryParam("callback") @DefaultValue("callback") String callback ) throws Exception {

        if ( applicationId == null ) {
            throw new IllegalArgumentException("Application ID not specified in request");
        }

        management.restoreApplication( applicationId );

        ApiResponse response = createApiResponse();
        response.setAction( "restore" );
        response.setApplication( emf.getEntityManager( applicationId ).getApplication() );
        response.setParams( ui.getQueryParameters() );

        return response;
    }


    @DELETE
    @RequireOrganizationAccess
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse executeDelete(  @Context UriInfo ui,
        @QueryParam("callback") @DefaultValue("callback") String callback,
        @QueryParam("app_delete_confirm") String confirmDelete) throws Exception {

        if (!"confirm_delete_of_application_and_data".equals( confirmDelete ) ) {
            throw new IllegalArgumentException(
                "Cannot delete application without app_delete_confirm parameter");
        }

        Properties props = management.getProperties();

        // for now, only works in test mode
        String testProp = ( String ) props.get( "usergrid.test" );
        if ( testProp == null || !Boolean.parseBoolean( testProp ) ) {
            throw new UnsupportedOperationException();
        }

        if ( applicationId == null ) {
            throw new IllegalArgumentException("Application ID not specified in request");
        }

        management.deleteApplication( applicationId );

        logger.debug( "ApplicationResource.delete() deleted appId = {}", applicationId);

        ApiResponse response = createApiResponse();
        response.setAction( "delete" );
        response.setApplication(emf.getEntityManager( applicationId ).getApplication());
        response.setParams(ui.getQueryParameters());

        logger.debug( "ApplicationResource.delete() sending response ");

        return response;
    }

}
