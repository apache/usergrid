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
package org.apache.usergrid.rest.management.users;


import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import net.tanesha.recaptcha.ReCaptchaImpl;
import net.tanesha.recaptcha.ReCaptchaResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.management.ActivationState;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.exceptions.RedirectionException;
import org.apache.usergrid.rest.management.ManagementResource;
import org.apache.usergrid.rest.management.users.organizations.OrganizationsResource;
import org.apache.usergrid.rest.security.annotations.RequireAdminUserAccess;
import org.apache.usergrid.security.shiro.utils.SubjectUtils;
import org.apache.usergrid.security.tokens.exceptions.TokenException;
import org.apache.usergrid.services.ServiceResults;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.Map;
import java.util.UUID;

import static org.apache.usergrid.security.shiro.utils.SubjectUtils.isServiceAdmin;
import static org.apache.usergrid.utils.ConversionUtils.string;


@Component( "org.apache.usergrid.rest.management.users.UserResource" )
@Scope( "prototype" )
@Produces( {
        MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
        "application/ecmascript", "text/jscript"
} )
public class UserResource extends AbstractContextResource {

    private static final Logger logger = LoggerFactory.getLogger( UserResource.class );

    UserInfo user;

    String errorMsg;

    String token;


    public UserResource() {
    }


    public UserResource init( UserInfo user ) {
        this.user = user;
        return this;
    }


    @RequireAdminUserAccess
    @Path( "organizations" )
    public OrganizationsResource getUserOrganizations( @Context UriInfo ui ) throws Exception {
        return getSubResource( OrganizationsResource.class ).init( user );
    }


    @RequireAdminUserAccess
    @Path( "orgs" )
    public OrganizationsResource getUserOrganizations2( @Context UriInfo ui ) throws Exception {
        return getSubResource( OrganizationsResource.class ).init( user );
    }


    @PUT
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse setUserInfo( @Context UriInfo ui, Map<String, Object> json,
                                        @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
            throws Exception {

        if ( json == null ) {
            return null;
        }
        if ( string( json.get( "oldpassword" ) ) != null ) {
            setUserPasswordPut( ui, json, callback );
            json.remove( "oldpassword" );
            json.remove( "newpassword" );
        }

        String email = string( json.remove( "email" ) );
        String username = string( json.remove( "username" ) );
        String name = string( json.remove( "name" ) );

        management.updateAdminUser( user, username, name, email, json );

        ApiResponse response = createApiResponse();
        response.setAction( "update user info" );

        return response;
    }


    @PUT
    @Path( "password" )
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse setUserPasswordPut( @Context UriInfo ui, Map<String, Object> json,
                                               @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
            throws Exception {

        if ( json == null ) {
            return null;
        }

        String oldPassword = string( json.get( "oldpassword" ) );
        String newPassword = string( json.get( "newpassword" ) );

        if ( isServiceAdmin() ) {
            management.setAdminUserPassword( user.getUuid(), newPassword );
        }
        else {
            management.setAdminUserPassword( user.getUuid(), oldPassword, newPassword );
        }

        ApiResponse response = createApiResponse();
        response.setAction( "set user password" );

        return response;
    }


    @POST
    @Path( "password" )
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse setUserPasswordPost( @Context UriInfo ui, Map<String, Object> json,
                                                @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
            throws Exception {
        return setUserPasswordPut( ui, json, callback );
    }


    @RequireAdminUserAccess
    @GET
    @Path( "feed" )
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse getFeed( @Context UriInfo ui,
                                    @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
            throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "get admin user feed" );

        ServiceResults results = management.getAdminUserActivity( user );
        response.setEntities( results.getEntities() );
        response.setSuccess();

        return response;
    }


    @RequireAdminUserAccess
    @GET
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse getUserData( @Context UriInfo ui, @QueryParam( "ttl" ) long ttl,
                                        @QueryParam( "shallow" ) boolean shallow,
                                        @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
            throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "get admin user" );

        String token = management.getAccessTokenForAdminUser( SubjectUtils.getUser().getUuid(), ttl );
        Map<String, Object> userOrganizationData = management.getAdminUserOrganizationData( user, !shallow );
        userOrganizationData.put( "token", token );
        response.setData( userOrganizationData );
        response.setSuccess();

        return response;
    }


    @GET
    @Path( "resetpw" )
    @Produces( MediaType.TEXT_HTML )
    public Viewable showPasswordResetForm( @Context UriInfo ui, @QueryParam( "token" ) String token ) {

        final boolean externalTokensEnabled =
                !StringUtils.isEmpty( properties.getProperty( ManagementResource.USERGRID_CENTRAL_URL ) );

        if ( externalTokensEnabled ) {
            throw new IllegalArgumentException( "Admin Users must reset passwords via " +
                    properties.getProperty( ManagementResource.USERGRID_CENTRAL_URL ) );
        }

        try {
            this.token = token;

            if ( management.checkPasswordResetTokenForAdminUser( user.getUuid(), token ) ) {
                return handleViewable( "resetpw_set_form", this );
            }
            else {
                return handleViewable( "resetpw_email_form", this );
            }
        }
        catch ( RedirectionException e ) {
            throw e;
        }
        catch ( Exception e ) {
            return handleViewable( "error", e );
        }
    }


    @POST
    @Path( "resetpw" )
    @Consumes( "application/x-www-form-urlencoded" )
    @Produces( MediaType.TEXT_HTML )
    public Viewable handlePasswordResetForm( @Context UriInfo ui, @FormParam( "token" ) String token,
                                             @FormParam( "password1" ) String password1,
                                             @FormParam( "password2" ) String password2,
                                             @FormParam( "recaptcha_challenge_field" ) String challenge,
                                             @FormParam( "recaptcha_response_field" ) String uresponse ) {

        logger.debug("handlePasswordResetForm");

        final boolean externalTokensEnabled =
                !StringUtils.isEmpty( properties.getProperty( ManagementResource.USERGRID_CENTRAL_URL ) );

        if ( externalTokensEnabled ) {
            throw new IllegalArgumentException( "Admin Users must reset passwords via " +
                    properties.getProperty( ManagementResource.USERGRID_CENTRAL_URL ) );
        }

        try {
            this.token = token;

            //      if(user == null) {
            //        errorMsg = "Incorrect username entered";
            //        return handleViewable("resetpw_set_form",this);
            //      }

            if ( ( password1 != null ) || ( password2 != null ) ) {
                if ( management.checkPasswordResetTokenForAdminUser( user.getUuid(), token ) ) {
                    if ( ( password1 != null ) && password1.equals( password2 ) ) {
                        management.setAdminUserPassword( user.getUuid(), password1 );
                        management.revokeAccessTokenForAdminUser( user.getUuid(), token );
                        return handleViewable( "resetpw_set_success", this );
                    }
                    else {
                        errorMsg = "Passwords didn't match, let's try again...";
                        return handleViewable( "resetpw_set_form", this );
                    }
                }
                else {
                    errorMsg = "Sorry, you have an invalid token. Let's try again...";
                    return handleViewable( "resetpw_email_form", this );
                }
            }

            if ( !useReCaptcha() ) {
                management.startAdminUserPasswordResetFlow( user );
                return handleViewable( "resetpw_email_success", this );
            }

            ReCaptchaImpl reCaptcha = new ReCaptchaImpl();
            reCaptcha.setPrivateKey( properties.getRecaptchaPrivate() );

            ReCaptchaResponse reCaptchaResponse =
                    reCaptcha.checkAnswer( httpServletRequest.getRemoteAddr(), challenge, uresponse );

            if ( reCaptchaResponse.isValid() ) {
                management.startAdminUserPasswordResetFlow( user );
                return handleViewable( "resetpw_email_success", this );
            }
            else {
                errorMsg = "Incorrect Captcha";
                return handleViewable( "resetpw_email_form", this );
            }
        }
        catch ( RedirectionException e ) {
            throw e;
        }
        catch ( Exception e ) {
            return handleViewable( "error", e );
        }
    }


    public String getErrorMsg() {
        return errorMsg;
    }


    public String getToken() {
        return token;
    }


    public UserInfo getUser() {
        return user;
    }


    @GET
    @Path( "activate" )
    @Produces( MediaType.TEXT_HTML )
    public Viewable activate( @Context UriInfo ui, @QueryParam( "token" ) String token ) {

        final boolean externalTokensEnabled =
                !StringUtils.isEmpty( properties.getProperty( ManagementResource.USERGRID_CENTRAL_URL ) );

        if ( externalTokensEnabled ) {
            throw new IllegalArgumentException( "Admin Users must activate via " +
                    properties.getProperty( ManagementResource.USERGRID_CENTRAL_URL ) );
        }

        try {
            management.handleActivationTokenForAdminUser( user.getUuid(), token );
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
    @Path( "confirm" )
    @Produces( MediaType.TEXT_HTML )
    public Viewable confirm( @Context UriInfo ui, @QueryParam( "token" ) String token ) {

        final boolean externalTokensEnabled =
                !StringUtils.isEmpty( properties.getProperty( ManagementResource.USERGRID_CENTRAL_URL ) );

        if ( externalTokensEnabled ) {
            throw new IllegalArgumentException( "Admin Users must confirm via " +
                    properties.getProperty( ManagementResource.USERGRID_CENTRAL_URL ) );
        }

        try {
            ActivationState state = management.handleConfirmationTokenForAdminUser( user.getUuid(), token );
            if ( state == ActivationState.CONFIRMED_AWAITING_ACTIVATION ) {
                return handleViewable( "confirm", this );
            }
            return handleViewable( "activate", this );
        }
        catch ( TokenException e ) {
            return handleViewable( "bad_confirmation_token", this );
        }
        catch ( RedirectionException e ) {
            throw e;
        }
        catch ( Exception e ) {
            return new Viewable( "error", e );
        }
    }


    @GET
    @Path( "reactivate" )
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse reactivate( @Context UriInfo ui,
                                       @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
            throws Exception {

        final boolean externalTokensEnabled =
                !StringUtils.isEmpty( properties.getProperty( ManagementResource.USERGRID_CENTRAL_URL ) );

        if ( externalTokensEnabled ) {
            throw new IllegalArgumentException( "Admin Users must reactivate via " +
                    properties.getProperty( ManagementResource.USERGRID_CENTRAL_URL ) );
        }

        logger.info( "Send activation email for user: {}" , user.getUuid() );

        ApiResponse response = createApiResponse();

        management.startAdminUserActivationFlow( user );

        response.setAction( "reactivate user" );
        return response;
    }


    @POST
    @Path( "revoketokens" )
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse revokeTokensPost( @Context UriInfo ui,
                                             @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
            throws Exception {

        UUID adminId = user.getUuid();

        logger.info( "Revoking user tokens for {}", adminId );

        ApiResponse response = createApiResponse();

        management.revokeAccessTokensForAdminUser( adminId );

        response.setAction( "revoked user tokens" );
        return response;
    }


    @PUT
    @Path( "revoketokens" )
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse revokeTokensPut( @Context UriInfo ui,
                                            @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
            throws Exception {
        return revokeTokensPost( ui, callback );
    }


    @POST
    @Path( "revoketoken" )
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse revokeTokenPost( @Context UriInfo ui,
                                            @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback,
                                            @QueryParam( "token" ) String token ) throws Exception {

        UUID adminId = user.getUuid();
        this.token = token;

        logger.info( "Revoking user tokens for {}", adminId );

        ApiResponse response = createApiResponse();

        management.revokeAccessTokenForAdminUser( adminId, token );

        response.setAction( "revoked user tokens" );
        return response;
    }


    @PUT
    @Path( "revoketoken" )
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse revokeTokenPut( @Context UriInfo ui,
                                           @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback,
                                           @QueryParam( "token" ) String token ) throws Exception {
        return revokeTokenPost( ui, callback, token );
    }
}
