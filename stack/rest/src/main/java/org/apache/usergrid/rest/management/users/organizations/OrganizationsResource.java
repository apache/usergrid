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
package org.apache.usergrid.rest.management.users.organizations;


import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.google.common.collect.BiMap;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.management.exceptions.ManagementException;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.RootResource;
import org.apache.usergrid.rest.security.annotations.RequireAdminUserAccess;
import org.apache.usergrid.rest.security.annotations.RequireOrganizationAccess;
import org.apache.usergrid.security.shiro.utils.SubjectUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.Map;
import java.util.UUID;


@Component( "org.apache.usergrid.rest.management.users.organizations.OrganizationsResource" )
@Scope( "prototype" )
@Produces( {
        MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
        "application/ecmascript", "text/jscript"
} )
public class OrganizationsResource extends AbstractContextResource {

    UserInfo user;


    public OrganizationsResource() {
    }


    public OrganizationsResource init( UserInfo user ) {
        this.user = user;
        return this;
    }


    @RequireAdminUserAccess
    @GET
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse getUserOrganizations( @Context UriInfo ui,
                                                 @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
            throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "get user management" );

        BiMap<UUID, String> userOrganizations = SubjectUtils.getOrganizations();
        response.setData( userOrganizations.inverse() );

        return response;
    }


    @RequireAdminUserAccess
    @POST
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse newOrganizationForUser( @Context UriInfo ui, Map<String, Object> json,
                                                   @QueryParam( "callback" ) @DefaultValue( "callback" )
                                                   String callback ) throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "new organization for user" );

        String organizationName = ( String ) json.get( "organization" );
        OrganizationInfo organization = management.createOrganization( organizationName, user, false );
        response.setData( organization );

        management.activateOrganization( organization );

        return response;
    }


    @RequireAdminUserAccess
    @POST
    @Consumes( MediaType.APPLICATION_FORM_URLENCODED )
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse newOrganizationForUserFromForm( @Context UriInfo ui, Map<String, Object> json,
                                                           @QueryParam( "callback" ) @DefaultValue( "callback" )
                                                           String callback,
                                                           @FormParam( "organization" ) String organizationName )
            throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "new organization for user" );

        if ( organizationName == null ) {
            throw new ManagementException( "Could not find organization for name: " + organizationName );
        }

        OrganizationInfo organization = management.createOrganization( organizationName, user, false );
        response.setData( organization );

        management.activateOrganization( organization );

        return response;
    }


    @RequireOrganizationAccess
    @PUT
    @Path( "{organizationName}" )
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse addUserToOrganizationByOrganizationName( @Context UriInfo ui,
                                                                    @PathParam( "organizationName" )
                                                                    String organizationName, @QueryParam( "callback" )
                                                                    @DefaultValue( "callback" ) String callback )
            throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "add user to organization" );

        OrganizationInfo organization = management.getOrganizationByName( organizationName );
        management.addAdminUserToOrganization( user, organization, true );
        response.setData( organization );
        return response;
    }


    @RequireOrganizationAccess
    @PUT
    @Path(RootResource.ORGANIZATION_ID_PATH)
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse addUserToOrganizationByOrganizationId( @Context UriInfo ui, @PathParam( "organizationId" )
    String organizationIdStr, @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback ) throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "add user to organization" );

        OrganizationInfo organization = management.getOrganizationByUuid( UUID.fromString( organizationIdStr ) );
        management.addAdminUserToOrganization( user, organization, true );
        response.setData( organization );
        return response;
    }


    @RequireOrganizationAccess
    @DELETE
    @Path( RootResource.ORGANIZATION_ID_PATH )
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse removeUserFromOrganizationByOrganizationId( @Context UriInfo ui,
                                                                       @PathParam( "organizationId" )
                                                                       String organizationIdStr,
                                                                       @QueryParam( "callback" )
                                                                       @DefaultValue( "callback" ) String callback )
            throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "remove user from organization" );

        OrganizationInfo organization = management.getOrganizationByUuid( UUID.fromString( organizationIdStr ) );
        management.removeAdminUserFromOrganization( user.getUuid(), organization.getUuid() );
        response.setData( organization );
        return response;
    }


    @RequireOrganizationAccess
    @DELETE
    @Path( "{organizationName}" )
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse removeUserFromOrganizationByOrganizationName( @Context UriInfo ui,
                                                                         @PathParam( "organizationName" )
                                                                         String organizationName,
                                                                         @QueryParam( "callback" )
                                                                         @DefaultValue( "callback" ) String callback )
            throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "remove user from organization" );
        OrganizationInfo organization = management.getOrganizationByName( organizationName );
        management.removeAdminUserFromOrganization( user.getUuid(), organization.getUuid() );
        response.setData( organization );

        return response;
    }
}
