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


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import org.apache.commons.lang.StringUtils;

import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.core.util.Health;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.security.annotations.RequireOrganizationAccess;
import org.apache.usergrid.security.oauth.ClientCredentialsInfo;
import org.apache.usergrid.security.providers.SignInAsProvider;
import org.apache.usergrid.security.providers.SignInProviderFactory;
import org.apache.usergrid.services.ServiceManager;

import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.google.common.base.Preconditions;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
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

    public static final String CONFIRM_APPLICATION_IDENTIFIER = "confirm_application_identifier";

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


    /**
     * Caller MUST pass confirm_application_identifier that is either the UUID or the
     * name of the application to be deleted. Yes, this is redundant and intended to
     * be a protection measure to force caller to confirm that they want to do a delete.
     */
    @DELETE
    @RequireOrganizationAccess
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse executeDelete(  @Context UriInfo ui,
        @QueryParam("callback") @DefaultValue("callback") String callback,
        @QueryParam(CONFIRM_APPLICATION_IDENTIFIER) String confirmApplicationIdentifier) throws Exception {

        if ( application == null && applicationId == null ) {
            throw new IllegalArgumentException("Application ID not specified in request");
        }

        // If the path uses name then expect name, otherwise if they use uuid then expect uuid.
        if (application == null) {
            if (!applicationId.toString().equals( confirmApplicationIdentifier )) {
                throw new IllegalArgumentException(
                    "Cannot delete application without supplying correct application id.");
            }

        } else if (!application.getName().split( "/" )[1].equals( confirmApplicationIdentifier ) ) {
            throw new IllegalArgumentException(
                "Cannot delete application without supplying correct application name");
        }

        management.deleteApplication( applicationId );

        if (logger.isTraceEnabled()) {
            logger.trace("ApplicationResource.delete() deleted appId = {}", applicationId);
        }

        ApiResponse response = createApiResponse();
        response.setAction( "delete" );
        response.setApplication(emf.getEntityManager( applicationId ).getApplication());
        response.setParams(ui.getQueryParameters());

        if (logger.isTraceEnabled()) {
            logger.trace("ApplicationResource.delete() sending response ");
        }

        return response;
    }

}
