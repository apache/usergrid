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
package org.apache.usergrid.rest.management.organizations.users;


import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.management.exceptions.ManagementException;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.RootResource;
import org.apache.usergrid.rest.security.annotations.RequireOrganizationAccess;
import org.apache.usergrid.security.shiro.utils.SubjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.apache.commons.collections.MapUtils.getObject;
import static org.apache.usergrid.rest.exceptions.SecurityException.mappableSecurityException;
import static org.apache.usergrid.utils.ConversionUtils.getBoolean;
import static org.apache.usergrid.utils.ConversionUtils.string;


@Component("org.apache.usergrid.rest.management.organizations.users.UsersResource")
@Scope("prototype")
@Produces({
        MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
        "application/ecmascript", "text/jscript"
})
public class UsersResource extends AbstractContextResource {

    private static final Logger logger = LoggerFactory.getLogger( UsersResource.class );

    OrganizationInfo organization;


    public UsersResource() {
    }


    public UsersResource init( OrganizationInfo organization ) {
        this.organization = organization;
        return this;
    }


    @RequireOrganizationAccess
    @GET
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse getOrganizationUsers( @Context UriInfo ui,
                                                 @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "get organization users" );

        List<UserInfo> users = management.getAdminUsersForOrganization( organization.getUuid() );
        response.setData( users );
        return response;
    }


    @RequireOrganizationAccess
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse newUserForOrganization( @Context UriInfo ui, Map<String, Object> json,
                                                   @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        String email = string( json.get( "email" ) );
        String username = string( json.get( "username" ) );
        String name = string( json.get( "name" ) );
        String password = string( json.get( "password" ) );
        boolean invite = getBoolean( getObject( json, "invite", true ) );

        return newUserForOrganizationFromForm( ui, username, name, email, password, invite, callback );
    }


    @RequireOrganizationAccess
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse newUserForOrganizationFromForm( @Context UriInfo ui, @FormParam("username") String username,
                                                           @FormParam("name") String name,
                                                           @FormParam("email") String email,
                                                           @FormParam("password") String password,
                                                           @FormParam("invite") @DefaultValue("true") boolean invite,
                                                           @QueryParam("callback") @DefaultValue("callback")
                                                           String callback ) throws Exception {

        logger.info( "New user for organization: " + username + " (" + email + ")");

        ApiResponse response = createApiResponse();
        response.setAction( "create user" );

        UserInfo user = null;
        if ( invite ) {
            user = management.getAdminUserByEmail( email );
        }

        if ( user == null ) {
            user = management.createAdminUser( username, name, email, password, false, false );

            // A null may be returned if the user fails validation check
            if ( user != null ) {
                management.startAdminUserPasswordResetFlow( user );
            }
        }

        if ( user == null ) {
            return null;
        }

        management.addAdminUserToOrganization( user, organization, true );

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put( "user", user );
        response.setData( result );
        response.setSuccess();

        return response;
    }

	/*
     * @RequireOrganizationAccess
	 *
	 * @POST
	 *
	 * @Consumes(MediaType.MULTIPART_FORM_DATA) public JSONWithPadding
	 * newUserForOrganizationFromMultipart(
	 *
	 * @Context UriInfo ui, @FormDataParam("username") String username,
	 *
	 * @FormDataParam("name") String name,
	 *
	 * @FormDataParam("email") String email,
	 *
	 * @FormDataParam("password") String password) throws Exception {
	 *
	 * return newUserForOrganizationFromForm(ui, username, name, email,
	 * password); }
	 */


    @RequireOrganizationAccess
    @PUT
    @Path(RootResource.USER_ID_PATH)
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse addUserToOrganization( @Context UriInfo ui, @PathParam("userId") String userIdStr,
                                                  @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "add user to organization" );

        UserInfo user = management.getAdminUserByUuid( UUID.fromString( userIdStr ) );
        if ( user == null ) {
            throw new ManagementException( "No user found for: " + userIdStr );
        }
        management.addAdminUserToOrganization( user, organization, true );

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put( "user", user );
        response.setData( result );
        response.setSuccess();

        return response;
    }


    @RequireOrganizationAccess
    @PUT
    @Path(RootResource.EMAIL_PATH)
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse addUserToOrganizationByEmail( @Context UriInfo ui, @PathParam("email") String email,
                                                         @QueryParam("callback") @DefaultValue("callback")
                                                         String callback ) throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "add user to organization" );

        UserInfo user = management.getAdminUserByEmail( email );
        if ( user == null ) {
            throw new ManagementException( "Username not found: " + email );
        }
        management.addAdminUserToOrganization( user, organization, true );

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put( "user", user );
        response.setData( result );
        response.setSuccess();

        return response;
    }


    @RequireOrganizationAccess
    @PUT
    @Path("{username}")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse addUserToOrganizationByUsername( @Context UriInfo ui, @PathParam("username") String username,
                                                            @QueryParam("callback") @DefaultValue("callback")
                                                            String callback ) throws Exception {

        if ( "me".equals( username ) ) {
            UserInfo user = SubjectUtils.getAdminUser();
            if ( ( user != null ) && ( user.getUuid() != null ) ) {
                return addUserToOrganization( ui, user.getUuid().toString(), callback );
            }
            throw mappableSecurityException( "unauthorized", "No admin identity for access credentials provided" );
        }

        ApiResponse response = createApiResponse();
        response.setAction( "add user to organization" );

        UserInfo user = management.getAdminUserByUsername( username );
        if ( user == null ) {
            throw new ManagementException( "Username not found: " + username );
        }
        management.addAdminUserToOrganization( user, organization, true );

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put( "user", user );
        response.setData( result );
        response.setSuccess();

        return response;
    }


    @RequireOrganizationAccess
    @DELETE
    @Path(RootResource.USER_ID_PATH)
    public ApiResponse removeUserFromOrganizationByUserId( @Context UriInfo ui,
                                                               @PathParam("userId") String userIdStr,
                                                               @QueryParam("callback") @DefaultValue("callback")
                                                               String callback ) throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "remove user from organization" );

        UserInfo user = management.getAdminUserByUuid( UUID.fromString( userIdStr ) );
        if ( user == null ) {
            return null;
        }
        management.removeAdminUserFromOrganization( user.getUuid(), organization.getUuid() );

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put( "user", user );
        response.setData( result );
        response.setSuccess();

        return response;
    }


    @RequireOrganizationAccess
    @DELETE
    @Path("{username}")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse removeUserFromOrganizationByUsername( @Context UriInfo ui,
                                                                 @PathParam("username") String username,
                                                                 @QueryParam("callback") @DefaultValue("callback")
                                                                 String callback ) throws Exception {

        if ( "me".equals( username ) ) {
            UserInfo user = SubjectUtils.getAdminUser();
            if ( ( user != null ) && ( user.getUuid() != null ) ) {
                return removeUserFromOrganizationByUserId( ui, user.getUuid().toString(), callback );
            }
            throw mappableSecurityException( "unauthorized", "No admin identity for access credentials provided" );
        }

        ApiResponse response = createApiResponse();
        response.setAction( "remove user from organization" );

        UserInfo user = management.getAdminUserByUsername( username );
        if ( user == null ) {
            return null;
        }
        management.removeAdminUserFromOrganization( user.getUuid(), organization.getUuid() );

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put( "user", user );
        response.setData( result );
        response.setSuccess();

        return response;
    }


    @RequireOrganizationAccess
    @DELETE
    @Path(RootResource.EMAIL_PATH)
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse removeUserFromOrganizationByEmail( @Context UriInfo ui, @PathParam("email") String email,
                                                              @QueryParam("callback") @DefaultValue("callback")
                                                              String callback ) throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "remove user from organization" );

        UserInfo user = management.getAdminUserByEmail( email );
        if ( user == null ) {
            return null;
        }
        management.removeAdminUserFromOrganization( user.getUuid(), organization.getUuid() );

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put( "user", user );
        response.setData( result );
        response.setSuccess();

        return response;
    }
}
