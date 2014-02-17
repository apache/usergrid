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


import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.apache.usergrid.management.ActivationState;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.exceptions.RedirectionException;
import org.apache.usergrid.rest.management.organizations.applications.ApplicationsResource;
import org.apache.usergrid.rest.management.organizations.users.UsersResource;
import org.apache.usergrid.rest.security.annotations.RequireOrganizationAccess;
import org.apache.usergrid.security.oauth.ClientCredentialsInfo;
import org.apache.usergrid.security.tokens.exceptions.TokenException;
import org.apache.usergrid.services.ServiceResults;

import com.sun.jersey.api.json.JSONWithPadding;
import com.sun.jersey.api.view.Viewable;


@Component("org.apache.usergrid.rest.management.organizations.OrganizationResource")
@Scope("prototype")
@Produces({
        MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
        "application/ecmascript", "text/jscript"
})
public class OrganizationResource extends AbstractContextResource {

    private static final Logger logger = LoggerFactory.getLogger( OrganizationsResource.class );

    OrganizationInfo organization;


    public OrganizationResource() {
    }


    public OrganizationResource init( OrganizationInfo organization ) {
        this.organization = organization;
        return this;
    }


    @RequireOrganizationAccess
    @Path("users")
    public UsersResource getOrganizationUsers( @Context UriInfo ui ) throws Exception {
        return getSubResource( UsersResource.class ).init( organization );
    }


    @RequireOrganizationAccess
    @Path("applications")
    public ApplicationsResource getOrganizationApplications( @Context UriInfo ui ) throws Exception {
        return getSubResource( ApplicationsResource.class ).init( organization );
    }


    @RequireOrganizationAccess
    @Path("apps")
    public ApplicationsResource getOrganizationApplications2( @Context UriInfo ui ) throws Exception {
        return getSubResource( ApplicationsResource.class ).init( organization );
    }


    @GET
    public JSONWithPadding getOrganizationDetails( @Context UriInfo ui,
                                                   @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        logger.info( "Get details for organization: " + organization.getUuid() );

        ApiResponse response = createApiResponse();
        response.setProperty( "organization", management.getOrganizationData( organization ) );

        return new JSONWithPadding( response, callback );
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
    public JSONWithPadding reactivate( @Context UriInfo ui,
                                       @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        logger.info( "Send activation email for organization: " + organization.getUuid() );

        ApiResponse response = createApiResponse();

        management.startOrganizationActivationFlow( organization );

        response.setAction( "reactivate organization" );
        return new JSONWithPadding( response, callback );
    }


    @RequireOrganizationAccess
    @GET
    @Path("feed")
    public JSONWithPadding getFeed( @Context UriInfo ui,
                                    @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "get organization feed" );

        ServiceResults results = management.getOrganizationActivity( organization );
        response.setEntities( results.getEntities() );
        response.setSuccess();

        return new JSONWithPadding( response, callback );
    }


    @RequireOrganizationAccess
    @GET
    @Path("credentials")
    public JSONWithPadding getCredentials( @Context UriInfo ui,
                                           @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "get organization client credentials" );

        ClientCredentialsInfo keys =
                new ClientCredentialsInfo( management.getClientIdForOrganization( organization.getUuid() ),
                        management.getClientSecretForOrganization( organization.getUuid() ) );

        response.setCredentials( keys );
        return new JSONWithPadding( response, callback );
    }


    @RequireOrganizationAccess
    @POST
    @Path("credentials")
    public JSONWithPadding generateCredentials( @Context UriInfo ui,
                                                @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "generate organization client credentials" );

        ClientCredentialsInfo credentials =
                new ClientCredentialsInfo( management.getClientIdForOrganization( organization.getUuid() ),
                        management.newClientSecretForOrganization( organization.getUuid() ) );

        response.setCredentials( credentials );
        return new JSONWithPadding( response, callback );
    }


    public OrganizationInfo getOrganization() {
        return organization;
    }


    @RequireOrganizationAccess
    @Consumes(MediaType.APPLICATION_JSON)
    @PUT
    public JSONWithPadding executePut( @Context UriInfo ui, Map<String, Object> json,
                                       @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        logger.debug( "OrganizationResource.executePut" );

        ApiResponse response = createApiResponse();
        response.setAction( "put" );

        response.setParams( ui.getQueryParameters() );

        Map customProperties = ( Map ) json.get( OrganizationsResource.ORGANIZATION_PROPERTIES );
        organization.setProperties( customProperties );
        management.updateOrganization( organization );

        return new JSONWithPadding( response, callback );
    }
}
