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
package org.apache.usergrid.rest.management.organizations;


import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.commons.lang.NullArgumentException;
import org.apache.usergrid.exception.NotImplementedException;
import org.apache.usergrid.management.ActivationState;
import org.apache.usergrid.management.OrganizationConfig;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.export.ExportService;
import org.apache.usergrid.persistence.entities.Export;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.applications.ServiceResource;
import org.apache.usergrid.rest.exceptions.RedirectionException;
import org.apache.usergrid.rest.management.organizations.applications.ApplicationsResource;
import org.apache.usergrid.rest.management.organizations.users.UsersResource;
import org.apache.usergrid.rest.security.annotations.RequireOrganizationAccess;
import org.apache.usergrid.rest.security.annotations.RequireSystemAccess;
import org.apache.usergrid.rest.utils.JSONPUtils;
import org.apache.usergrid.security.oauth.ClientCredentialsInfo;
import org.apache.usergrid.security.tokens.exceptions.TokenException;
import org.apache.usergrid.services.ServiceResults;
import org.glassfish.jersey.server.mvc.Viewable;
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
import java.util.*;

import static javax.servlet.http.HttpServletResponse.*;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;


@Component("org.apache.usergrid.rest.management.organizations.OrganizationResource")
@Scope("prototype")
@Produces({
        MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
        "application/ecmascript", "text/jscript"
})
public class OrganizationResource extends AbstractContextResource {

    private static final Logger logger = LoggerFactory.getLogger( OrganizationsResource.class );

    @Autowired
    protected ExportService exportService;

    OrganizationInfo organization;


    public OrganizationResource() {
        if (logger.isDebugEnabled()) {
            logger.debug("OrganizationResource created");
        }
    }


    public OrganizationResource init( OrganizationInfo organization ) {
        this.organization = organization;
        if (logger.isDebugEnabled()) {
            logger.debug("OrganizationResource initialized for org {}", organization.getName());
        }
        return this;
    }


    @Path("users")
    public UsersResource getOrganizationUsers( @Context UriInfo ui ) throws Exception {
        return getSubResource( UsersResource.class ).init( organization );
    }


    @Path("applications")
    public ApplicationsResource getOrganizationApplications( @Context UriInfo ui ) throws Exception {
        return getSubResource( ApplicationsResource.class ).init( organization );
    }


    @Path("apps")
    public ApplicationsResource getOrganizationApplications2( @Context UriInfo ui ) throws Exception {
        return getSubResource( ApplicationsResource.class ).init( organization );
    }


    @GET
    @JSONP
    @RequireOrganizationAccess
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse getOrganizationDetails( @Context UriInfo ui,
                                                   @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        logger.info( "Get details for organization: " + organization.getUuid() );

        ApiResponse response = createApiResponse();
        response.setProperty( "organization", management.getOrganizationData( organization ) );

        return response;
    }


    @GET
    @Path("activate")
    @Produces(MediaType.TEXT_HTML)
    public Viewable activate( @Context UriInfo ui, @QueryParam("token") String token ) {

        try {
            management.handleActivationTokenForOrganization( organization.getUuid(), token );
            return handleViewable( "activate", this );
        }
        catch ( TokenException e ) {
            return handleViewable( "bad_activation_token", this );
        }
        catch ( RedirectionException e ) {
            throw e;
        }
        catch ( Exception e ) {
            return handleViewable( "error", e );
        }
    }


    @GET
    @Path("confirm")
    @Produces(MediaType.TEXT_HTML)
    public Viewable confirm( @Context UriInfo ui, @QueryParam("token") String token ) {

        try {
            ActivationState state = management.handleActivationTokenForOrganization( organization.getUuid(), token );
            if ( state == ActivationState.CONFIRMED_AWAITING_ACTIVATION ) {
                return handleViewable( "confirm", this );
            }
            return handleViewable( "activate", this );
        }
        catch ( TokenException e ) {
            return handleViewable( "bad_activation_token", this );
        }
        catch ( RedirectionException e ) {
            throw e;
        }
        catch ( Exception e ) {
            return handleViewable( "error", e );
        }
    }


    @GET
    @Path("reactivate")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse reactivate( @Context UriInfo ui,
                                       @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        logger.info( "Send activation email for organization: " + organization.getUuid() );

        ApiResponse response = createApiResponse();

        management.startOrganizationActivationFlow( organization );

        response.setAction( "reactivate organization" );
        return response;
    }


    @RequireOrganizationAccess
    @GET
    @Path("feed")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse getFeed( @Context UriInfo ui,
                                    @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "get organization feed" );

        ServiceResults results = management.getOrganizationActivity( organization );
        response.setEntities( results.getEntities() );
        response.setSuccess();

        return response;
    }


    @RequireOrganizationAccess
    @GET
    @Path("credentials")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse getCredentials( @Context UriInfo ui,
                                           @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "get organization client credentials" );

        ClientCredentialsInfo keys =
                new ClientCredentialsInfo( management.getClientIdForOrganization( organization.getUuid() ),
                        management.getClientSecretForOrganization( organization.getUuid() ) );

        response.setCredentials( keys );
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
        response.setAction( "generate organization client credentials" );

        ClientCredentialsInfo credentials =
                new ClientCredentialsInfo( management.getClientIdForOrganization( organization.getUuid() ),
                        management.newClientSecretForOrganization( organization.getUuid() ) );

        response.setCredentials( credentials );
        return response;
    }


    public OrganizationInfo getOrganization() {
        return organization;
    }


    @RequireOrganizationAccess
    @Consumes(MediaType.APPLICATION_JSON)
    @PUT
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse executePut( @Context UriInfo ui, Map<String, Object> json,
                                       @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        if (logger.isDebugEnabled()) {
            logger.debug("executePut");
        }

        ApiResponse response = createApiResponse();
        response.setAction( "put" );

        response.setParams( ui.getQueryParameters() );

        Map customProperties = ( Map ) json.get( OrganizationsResource.ORGANIZATION_PROPERTIES );
        organization.setProperties( customProperties );
        management.updateOrganization( organization );

        return response;
    }

    @POST
    @Path("export")
    @Consumes(APPLICATION_JSON)
    @RequireOrganizationAccess
    public Response exportPostJson( @Context UriInfo ui,Map<String, Object> json,
                                    @QueryParam("callback") @DefaultValue("") String callback )
            throws OAuthSystemException {

        if (logger.isDebugEnabled()) {
            logger.debug("executePostJson");
        }

        Map<String, String> uuidRet = new HashMap<>();

        try {
            Object propertiesObj = json.get("properties");
            if (propertiesObj == null) {
                throw new NullArgumentException("Could not find 'properties'");
            }
            if (!(propertiesObj instanceof Map)) {
                throw new IllegalArgumentException("'properties' not a map");
            }

            @SuppressWarnings("unchecked")
            Map<String,Object> properties = (Map<String,Object>)propertiesObj;

            String storage_provider = ( String ) properties.get( "storage_provider" );
            if(storage_provider == null) {
                throw new NullArgumentException( "Could not find field 'storage_provider'" );
            }

            Object storageInfoObj = properties.get("storage_info");
            if(storageInfoObj == null) {
                throw new NullArgumentException( "Could not find field 'storage_info'" );
            }
            @SuppressWarnings("unchecked")
            Map<String,Object> storage_info = (Map<String, Object>)storageInfoObj;

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

            json.put( "organizationId",organization.getUuid());

            UUID jobUUID = exportService.schedule( json );
            uuidRet.put( "Export Entity", jobUUID.toString() );
        }
        catch ( NullArgumentException e ) {
            return Response.status( SC_BAD_REQUEST ).type( JSONPUtils.jsonMediaType( callback ) )
                           .entity( ServiceResource.wrapWithCallback( e.getMessage(), callback ) ).build();
        }
        catch ( Exception e ) {
            //TODO:throw descriptive error message and or include on in the response
            //TODO:fix below, it doesn't work if there is an exception. Make it look like the OauthResponse.
            return Response.status(  SC_INTERNAL_SERVER_ERROR ).type( JSONPUtils.jsonMediaType( callback ) )
                           .entity( ServiceResource.wrapWithCallback( e.getMessage(), callback ) ).build();
        }
        return Response.status( SC_ACCEPTED ).entity( uuidRet ).build();
    }

    @GET
    @RequireOrganizationAccess
    @Path("export/{exportEntity: [A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}}")
    public Response exportGetJson( @Context UriInfo ui, @PathParam("exportEntity") UUID exportEntityUUIDStr,
                                   @QueryParam("callback") @DefaultValue("") String callback ) throws Exception {

        Export entity;
        try {
            entity = smf.getServiceManager( emf.getManagementAppId() ).getEntityManager()
                        .get( exportEntityUUIDStr, Export.class );
        }
        catch ( Exception e ) { //this might not be a bad request and needs better error checking
            return Response.status( SC_BAD_REQUEST ).type( JSONPUtils.jsonMediaType( callback ) )
                           .entity( ServiceResource.wrapWithCallback( e.getMessage(), callback ) ).build();
        }

        if ( entity == null ) {
            return Response.status( SC_BAD_REQUEST ).build();
        }

        return Response.status( SC_OK ).entity( entity).build();
    }


    protected Set<String> getSetFromCommaSeparatedString(String input) {
        Set<String> ret = new HashSet<>();
        StringTokenizer tokenizer = new StringTokenizer(input, ",");

        while (tokenizer.hasMoreTokens()) {
            ret.add(tokenizer.nextToken());
        }

        return ret;
    }


    protected Map<String, Object> getConfigData(OrganizationConfig orgConfig, String itemsParam,
                                                boolean includeDefaults, boolean includeOverrides) {
        boolean itemsParamEmpty = itemsParam == null || itemsParam.isEmpty() || itemsParam.equals("*");
        return orgConfig.getOrgConfigCustomMap(itemsParamEmpty ? null : getSetFromCommaSeparatedString(itemsParam),
                includeDefaults, includeOverrides);
    }


    @JSONP
    @RequireSystemAccess
    @GET
    @Path("config")
    public ApiResponse getConfig( @Context UriInfo ui,
                                  @QueryParam("items") @DefaultValue("") String itemsParam,
                                  @QueryParam("separate_defaults") @DefaultValue("false") boolean separateDefaults,
                                  @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        logger.info( "Get configuration for organization: " + organization.getUuid() );

        ApiResponse response = createApiResponse();
        response.setAction( "get organization configuration" );
        //response.setParams(ui.getQueryParameters());

        OrganizationConfig orgConfig =
                management.getOrganizationConfigByUuid( organization.getUuid() );

        if (separateDefaults) {
            response.setProperty("orgConfiguration", getConfigData(orgConfig, itemsParam, false, true));
            response.setProperty("defaults", getConfigData(orgConfig, itemsParam, true, false));
        } else {
            response.setProperty("configuration", getConfigData(orgConfig, itemsParam, true, true));
        }

        return response;
    }


    @RequireSystemAccess
    @Consumes(MediaType.APPLICATION_JSON)
    @JSONP
    @PUT
    @Path("config")
    public ApiResponse putConfig( @Context UriInfo ui,
                                  Map<String, Object> json,
                                  @QueryParam("separate_defaults") @DefaultValue("false") boolean separateDefaults,
                                  @QueryParam("only_changed") @DefaultValue("false") boolean onlyChanged,
                                  @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        if (logger.isDebugEnabled()) {
            logger.debug("Put configuration for organization: " + organization.getUuid());
        }

        ApiResponse response = createApiResponse();
        response.setAction("put organization configuration");
        //response.setParams(ui.getQueryParameters());

        OrganizationConfig orgConfig =
                management.getOrganizationConfigByUuid( organization.getUuid() );

        // validates JSON and throws IllegalArgumentException if invalid
        // exception will be handled up the chain
        orgConfig.addProperties(json, true);

        management.updateOrganizationConfig(orgConfig);

        // refresh orgConfig -- to pick up removed entries and defaults
        orgConfig = management.getOrganizationConfigByUuid( organization.getUuid() );

        String itemsToReturn = "";
        if (onlyChanged) {
            itemsToReturn = String.join(",", json.keySet());
        }

        if (separateDefaults) {
            response.setProperty("orgConfiguration", getConfigData(orgConfig, itemsToReturn, false, true));
            response.setProperty("defaults", getConfigData(orgConfig, itemsToReturn, true, false));
        } else {
            response.setProperty( "configuration", getConfigData(orgConfig, itemsToReturn, true, true));
        }

        return response;
    }


    /** Delete organization is not yet supported */
    //@RequireSystemAccess
    @DELETE
    public ApiResponse deleteOrganization() throws Exception {
        throw new NotImplementedException();
    }

}
