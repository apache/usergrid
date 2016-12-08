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
package org.apache.usergrid.rest.applications.users;


import java.util.Map;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.usergrid.rest.security.annotations.CheckPermissionsForPath;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.common.message.OAuthResponse;
import org.apache.commons.lang.StringUtils;

import org.apache.usergrid.management.ActivationState;
import org.apache.usergrid.persistence.CredentialsInfo;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.applications.ServiceResource;
import org.apache.usergrid.rest.exceptions.RedirectionException;
import org.apache.usergrid.rest.security.annotations.RequireApplicationAccess;
import org.apache.usergrid.rest.security.annotations.RequireSystemAccess;
import org.apache.usergrid.security.oauth.AccessInfo;
import org.apache.usergrid.security.tokens.exceptions.TokenException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;

import net.tanesha.recaptcha.ReCaptchaImpl;
import net.tanesha.recaptcha.ReCaptchaResponse;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import static org.apache.usergrid.security.shiro.utils.SubjectUtils.*;
import static org.apache.usergrid.utils.ConversionUtils.string;


@Component("org.apache.usergrid.rest.applications.users.UserResource")
@Scope("prototype")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource extends ServiceResource {

    public static final String USER_EXTENSION_RESOURCE_PREFIX = "org.apache.usergrid.rest.applications.users.extensions.";

    private static final Logger logger = LoggerFactory.getLogger( UserResource.class );

    User user;

    Identifier userIdentifier;

    String errorMsg;

    String token;


    public UserResource() {
    }


    public UserResource init( Identifier userIdentifier ) throws Exception {
        this.userIdentifier = userIdentifier;
        return this;
    }


    @CheckPermissionsForPath
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse executePut( @Context UriInfo ui, String body,
                                       @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> json = mapper.readValue( body, mapTypeReference );

        if ( json != null ) {

            if ( "me".equals( json.get("username") ) ) {
                throw new IllegalArgumentException( "Username 'me' is reserved" );
            }

            json.remove( "password" );
            json.remove( "pin" );
        }

        return super.executePutWithMap( ui, json, callback );
    }

    // no access token needed
    @PUT
    @Path("password")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse setUserPasswordPut( @Context UriInfo ui, Map<String, Object> json,
                                               @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        if (logger.isTraceEnabled()) {
            logger.trace("UserResource.setUserPassword");
        }

        if ( json == null ) {
            return null;
        }

        ApiResponse response = createApiResponse();
        response.setAction( "set user password" );
        String oldPassword = string( json.get( "oldpassword" ) );
        String newPassword = string( json.get( "newpassword" ) );

        if ( newPassword == null ) {
            throw new IllegalArgumentException( "newpassword is required" );
        }

        UUID applicationId = getApplicationId();
        UUID targetUserId = getUserUuid();

        if ( targetUserId == null ) {
            response.setError( "User not found" );
            return response;
        }


        if ( isApplicationAdmin() ) {

            management.setAppUserPassword( applicationId, targetUserId, newPassword );
        }

        // we're not an admin user, we can only update the password ourselves
        else {
            management.setAppUserPassword( getApplicationId(), targetUserId, oldPassword, newPassword );
        }

        return response;
    }

    @GET
    @RequireSystemAccess
    @Path("credentials")
    public ApiResponse getUserCredentials(@QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        if (logger.isTraceEnabled()) {
            logger.trace("UserResource.getUserCredentials");
        }


        final ApiResponse response = createApiResponse();
        response.setAction( "get user credentials" );

        final UUID applicationId = getApplicationId();
        final UUID targetUserId = getUserUuid();

        if ( applicationId == null ) {
            response.setError( "Application not found" );
            return response;
        }

        if ( targetUserId == null ) {
            response.setError( "User not found" );
            return response;
        }

        final CredentialsInfo credentialsInfo = management.getAppUserCredentialsInfo( applicationId, targetUserId );


        response.setProperty( "credentials", credentialsInfo );


        return response;
    }



    @PUT
    @RequireSystemAccess
    @Path("credentials")
    public ApiResponse setUserCredentials( @Context UriInfo ui, Map<String, Object> json,
                                               @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        if (logger.isTraceEnabled()) {
            logger.trace("UserResource.setUserCredentials");
        }

        if ( json == null ) {
            return null;
        }

        if ( "me".equals( json.get("username") ) ) {
            throw new IllegalArgumentException( "Username 'me' is reserved" );
        }

        ApiResponse response = createApiResponse();
        response.setAction( "set user credentials" );

        @SuppressWarnings("unchecked")
        Map<String, Object> credentialsJson = ( Map<String, Object> ) json.get( "credentials" );


        if ( credentialsJson == null ) {
            throw new IllegalArgumentException( "credentials sub object is required" );
        }

        final CredentialsInfo credentials = CredentialsInfo.fromJson( credentialsJson );

        UUID applicationId = getApplicationId();
        UUID targetUserId = getUserUuid();

        if ( targetUserId == null ) {
            response.setError( "User not found" );
            return response;
        }


        management.setAppUserCredentialsInfo( applicationId, targetUserId, credentials );


        return response;
    }


    // no access token needed
    @POST
    @Path("password")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse setUserPasswordPost( @Context UriInfo ui, Map<String, Object> json,
                                                @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {
        return setUserPasswordPut( ui, json, callback );
    }


    @CheckPermissionsForPath
    @POST
    @Path("deactivate")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse deactivate( @Context UriInfo ui, Map<String, Object> json,
                                       @QueryParam("callback") @DefaultValue("") String callback ) throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "Deactivate user" );

        User user = management.deactivateUser( getApplicationId(), getUserUuid() );

        response.withEntity( user );

        return response;
    }


    @CheckPermissionsForPath
    @GET
    @Path("sendpin")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse sendPin( @Context UriInfo ui,
                                    @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        if (logger.isTraceEnabled()) {
            logger.trace("UserResource.sendPin");
        }

        ApiResponse response = createApiResponse();
        response.setAction( "retrieve user pin" );

        if ( getUser() != null ) {
            management.sendAppUserPin( getApplicationId(), getUserUuid() );
        }
        else {
            response.setError( "User not found" );
        }

        return response;
    }


    @CheckPermissionsForPath
    @POST
    @Path("sendpin")
    @JSONP
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse postSendPin( @Context UriInfo ui,
                                        @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {
        return sendPin( ui, callback );
    }


    @CheckPermissionsForPath
    @GET
    @Path("setpin")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse setPin( @Context UriInfo ui, @QueryParam("pin") String pin,
                                   @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        if (logger.isTraceEnabled()) {
            logger.trace("UserResource.setPin");
        }

        ApiResponse response = createApiResponse();
        response.setAction( "set user pin" );

        if ( getUser() != null ) {
            management.setAppUserPin( getApplicationId(), getUserUuid(), pin );
        }
        else {
            response.setError( "User not found" );
        }

        return response;
    }


    @CheckPermissionsForPath
    @POST
    @Path("setpin")
    @Consumes("application/x-www-form-urlencoded")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse postPin( @Context UriInfo ui, @FormParam("pin") String pin,
                                    @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        if (logger.isTraceEnabled()) {
            logger.trace("UserResource.postPin");
        }

        ApiResponse response = createApiResponse();
        response.setAction( "set user pin" );

        if ( getUser() != null ) {
            management.setAppUserPin( getApplicationId(), getUserUuid(), pin );
        }
        else {
            response.setError( "User not found" );
        }

        return response;
    }


    @CheckPermissionsForPath
    @POST
    @Path("setpin")
    @Consumes(MediaType.APPLICATION_JSON)
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse jsonPin( @Context UriInfo ui, JsonNode json,
                                    @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        if (logger.isTraceEnabled()) {
            logger.trace("UserResource.jsonPin");
        }
        ApiResponse response = createApiResponse();
        response.setAction( "set user pin" );

        if ( getUser() != null ) {
            String pin = json.path( "pin" ).textValue();
            management.setAppUserPin( getApplicationId(), getUserUuid(), pin );
        }
        else {
            response.setError( "User not found" );
        }

        return response;
    }


    // no access token needed
    @GET
    @Path("resetpw")
    @Produces(MediaType.TEXT_HTML)
    public Viewable showPasswordResetForm( @Context UriInfo ui, @QueryParam("token") String token ) {

        if (logger.isTraceEnabled()) {
            logger.trace( "UserResource.showPasswordResetForm" );
        }

        this.token = token;
        try {
            if ( management.checkPasswordResetTokenForAppUser( getApplicationId(), getUserUuid(), token ) ) {
                return handleViewable( "resetpw_set_form", this, getOrganizationName() );
            }
            else {
                return handleViewable( "resetpw_email_form", this, getOrganizationName() );
            }
        }
        catch ( RedirectionException e ) {
            throw e;
        }
        catch ( Exception e ) {
            return handleViewable( "error", e, getOrganizationName() );
        }
    }


    // no access token needed, reset token required
    @POST
    @Path("resetpw")
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.TEXT_HTML)
    public Viewable handlePasswordResetForm( @Context UriInfo ui, @FormParam("token") String token,
                                             @FormParam("password1") String password1,
                                             @FormParam("password2") String password2,
                                             @FormParam("recaptcha_challenge_field") String challenge,
                                             @FormParam("recaptcha_response_field") String uresponse ) {

        try {
            if (logger.isTraceEnabled()) {
                logger.trace("UserResource.handlePasswordResetForm");
            }

            this.token = token;

            if ( ( password1 != null ) || ( password2 != null ) ) {
                if ( management.checkPasswordResetTokenForAppUser( getApplicationId(), getUserUuid(), token ) ) {
                    if ( ( password1 != null ) && password1.equals( password2 ) ) {
                        management.setAppUserPassword( getApplicationId(), getUser().getUuid(), password1 );
                        management.revokeAccessTokenForAppUser( token );
                        return handleViewable( "resetpw_set_success", this, getOrganizationName() );
                    }
                    else {
                        errorMsg = "Passwords didn't match, let's try again...";
                        return handleViewable( "resetpw_set_form", this, getOrganizationName() );
                    }
                }
                else {
                    errorMsg = "Sorry, you have an invalid token. Let's try again...";
                    return handleViewable( "resetpw_email_form", this, getOrganizationName() );
                }
            }

            if ( !useReCaptcha() ) {
                management.startAppUserPasswordResetFlow( getApplicationId(), getUser() );
                return handleViewable( "resetpw_email_success", this, getOrganizationName() );
            }

            ReCaptchaImpl reCaptcha = new ReCaptchaImpl();
            reCaptcha.setPrivateKey( properties.getRecaptchaPrivate() );

            ReCaptchaResponse reCaptchaResponse =
                    reCaptcha.checkAnswer( httpServletRequest.getRemoteAddr(), challenge, uresponse );

            if ( reCaptchaResponse.isValid() ) {
                management.startAppUserPasswordResetFlow( getApplicationId(), getUser() );
                return handleViewable( "resetpw_email_success", this, getOrganizationName() );
            }
            else {
                errorMsg = "Incorrect Captcha";
                return handleViewable( "resetpw_email_form", this, getOrganizationName() );
            }
        }
        catch ( RedirectionException e ) {
            throw e;
        }
        catch ( Exception e ) {
            return handleViewable( "error", e, getOrganizationName() );
        }
    }


    public String getErrorMsg() {
        return errorMsg;
    }


    public String getToken() {
        return token;
    }


    public User getUser() {
        if ( user == null ) {
            EntityManager em = getServices().getEntityManager();
            try {
                user = em.get( em.getUserByIdentifier( userIdentifier ), User.class );
            }
            catch ( Exception e ) {
                logger.error( "Unable go get user", e );
            }
        }
        return user;
    }


    public UUID getUserUuid() {
        user = getUser();
        if ( user == null ) {
            return null;
        }
        return user.getUuid();
    }


    // no access token needed, activation token required
    @GET
    @Path("activate")
    @Produces(MediaType.TEXT_HTML)
    public Viewable activate( @Context UriInfo ui, @QueryParam("token") String token ) {

        try {
            management.handleActivationTokenForAppUser( getApplicationId(), getUserUuid(), token );
            return handleViewable( "activate", this, getOrganizationName() );
        }
        catch ( TokenException e ) {
            return handleViewable( "bad_activation_token", this, getOrganizationName() );
        }
        catch ( RedirectionException e ) {
            throw e;
        }
        catch ( Exception e ) {
            return handleViewable( "error", e, getOrganizationName() );
        }
    }


    // no access token needed, confirmation token required
    @GET
    @Path("confirm")
    @Produces(MediaType.TEXT_HTML)
    public Viewable confirm( @Context UriInfo ui, @QueryParam("token") String token ) {

        try {
            ActivationState state =
                    management.handleConfirmationTokenForAppUser( getApplicationId(), getUserUuid(), token );
            if ( state == ActivationState.CONFIRMED_AWAITING_ACTIVATION ) {
                return handleViewable( "confirm", this, getOrganizationName() );
            }
            return handleViewable( "activate", this, getOrganizationName() );
        }
        catch ( TokenException e ) {
            return handleViewable( "bad_confirmation_token", this, getOrganizationName() );
        }
        catch ( RedirectionException e ) {
            throw e;
        }
        catch ( Exception e ) {
            return handleViewable( "error", e, getOrganizationName() );
        }
    }


    @GET
    @Path("reactivate")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse reactivate( @Context UriInfo ui,
                                       @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        if (logger.isTraceEnabled()) {
            logger.trace("Send activation email for user: {}", getUserUuid());
        }

        ApiResponse response = createApiResponse();

        management.startAppUserActivationFlow( getApplicationId(), user );

        response.setAction( "reactivate user" );
        return response;
    }


    @POST
    @Path("revoketokens")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse revokeTokensPost( @Context UriInfo ui,
                                             @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        if (logger.isTraceEnabled()) {
            logger.trace("Revoking user tokens for {}", getUserUuid());
        }

        ApiResponse response = createApiResponse();

        management.revokeAccessTokensForAppUser( getApplicationId(), getUserUuid() );

        response.setAction( "revoked user tokens" );
        return response;
    }


    @CheckPermissionsForPath
    @PUT
    @Path("revoketokens")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse revokeTokensPut( @Context UriInfo ui,
                                            @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {
        return revokeTokensPost( ui, callback );
    }


    @CheckPermissionsForPath
    @POST
    @Path("revoketoken")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse revokeTokenPost( @Context UriInfo ui,
                                            @QueryParam("callback") @DefaultValue("callback") String callback,
                                            @QueryParam("token") String token ) throws Exception {

        if (logger.isTraceEnabled()) {
            logger.trace( "Revoking user token for {}",  getUserUuid() );
        }

        ApiResponse response = createApiResponse();

        management.revokeAccessTokenForAppUser( token );

        response.setAction( "revoked user token" );
        return response;
    }


    @CheckPermissionsForPath
    @PUT
    @Path("revoketoken")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse revokeTokenPut( @Context UriInfo ui,
                                           @QueryParam("callback") @DefaultValue("callback") String callback,
                                           @QueryParam("token") String token ) throws Exception {
        return revokeTokenPost( ui, callback, token );
    }


    @CheckPermissionsForPath
    @GET
    @Path("token")
    @RequireApplicationAccess
    public Response getAccessToken( @Context UriInfo ui, @QueryParam("ttl") long ttl,
                                    @QueryParam("callback") @DefaultValue("") String callback ) throws Exception {

        if (logger.isTraceEnabled()) {
            logger.trace("UserResource.getAccessToken");
        }

        try {

            // don't allow application user tokens to be exchanged for new tokens (possibly increasing ttl)
            if ( isApplicationUser() ) {
                OAuthResponse res = OAuthResponse.errorResponse( SC_FORBIDDEN ).buildJSONMessage();
                return Response.status( res.getResponseStatus() ).type( jsonMediaType( callback ) )
                               .entity( wrapWithCallback( res.getBody(), callback ) ).build();
            }

            String token = management.getAccessTokenForAppUser( services.getApplicationId(), getUserUuid(), ttl );

            AccessInfo access_info =
                    new AccessInfo().withExpiresIn( tokens.getMaxTokenAgeInSeconds( token ) ).withAccessToken( token )
                                    .withProperty( "user", getUser() );

            return Response.status( SC_OK ).type( jsonMediaType( callback ) )
                           .entity( wrapWithCallback( access_info, callback ) ).build();
        }
        catch ( OAuthProblemException e ) {
            logger.error( "OAuth Error", e );
            OAuthResponse res = OAuthResponse.errorResponse( SC_BAD_REQUEST ).error( e ).buildJSONMessage();
            return Response.status( res.getResponseStatus() ).type( jsonMediaType( callback ) )
                           .entity( wrapWithCallback( res.getBody(), callback ) ).build();
        }
    }


    @Override
    @Path("{itemName}")
    public AbstractContextResource addNameParameter( @Context UriInfo ui, @PathParam("itemName") PathSegment itemName )
            throws Exception {

        // check for user extension
        String resourceClass =
                USER_EXTENSION_RESOURCE_PREFIX + StringUtils.capitalize( itemName.getPath() ) + "Resource";
        AbstractUserExtensionResource extensionResource = null;
        try {
            @SuppressWarnings("unchecked") Class<AbstractUserExtensionResource> extensionCls =
                    ( Class<AbstractUserExtensionResource> ) Class.forName( resourceClass );
            extensionResource = getSubResource( extensionCls );
        }
        catch ( Exception e ) {
            // intentionally empty
        }
        if ( extensionResource != null ) {
            return extensionResource;
        }

        return super.addNameParameter( ui, itemName );
    }
}
