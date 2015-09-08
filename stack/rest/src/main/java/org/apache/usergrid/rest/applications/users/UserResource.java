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


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import net.tanesha.recaptcha.ReCaptchaImpl;
import net.tanesha.recaptcha.ReCaptchaResponse;
import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.common.message.OAuthResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.management.ActivationState;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.applications.ServiceResource;
import org.apache.usergrid.rest.exceptions.RedirectionException;
import org.apache.usergrid.rest.security.annotations.RequireApplicationAccess;
import org.apache.usergrid.security.oauth.AccessInfo;
import org.apache.usergrid.security.tokens.exceptions.TokenException;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.Map;
import java.util.UUID;

import static javax.servlet.http.HttpServletResponse.*;
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


    @PUT
    @RequireApplicationAccess
    @Consumes(MediaType.APPLICATION_JSON)
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse executePut( @Context UriInfo ui, String body,
                                       @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> json = mapper.readValue( body, mapTypeReference );

        if ( json != null ) {
            json.remove( "password" );
            json.remove( "pin" );
        }

        return super.executePutWithMap( ui, json, callback );
    }


    @PUT
    @Path("password")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse setUserPasswordPut( @Context UriInfo ui, Map<String, Object> json,
                                               @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        logger.info( "UserResource.setUserPassword" );

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


    @POST
    @Path("password")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse setUserPasswordPost( @Context UriInfo ui, Map<String, Object> json,
                                                @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {
        return setUserPasswordPut( ui, json, callback );
    }


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


    @GET
    @Path("sendpin")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse sendPin( @Context UriInfo ui,
                                    @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        logger.info( "UserResource.sendPin" );

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


    @POST
    @Path("sendpin")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse postSendPin( @Context UriInfo ui,
                                        @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {
        return sendPin( ui, callback );
    }


    @GET
    @Path("setpin")
    @RequireApplicationAccess
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse setPin( @Context UriInfo ui, @QueryParam("pin") String pin,
                                   @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        logger.info( "UserResource.setPin" );

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


    @POST
    @Path("setpin")
    @Consumes("application/x-www-form-urlencoded")
    @RequireApplicationAccess
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse postPin( @Context UriInfo ui, @FormParam("pin") String pin,
                                    @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        logger.info( "UserResource.postPin" );

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


    @POST
    @Path("setpin")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequireApplicationAccess
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse jsonPin( @Context UriInfo ui, JsonNode json,
                                    @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        logger.info( "UserResource.jsonPin" );
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


    @GET
    @Path("resetpw")
    @Produces(MediaType.TEXT_HTML)
    public Viewable showPasswordResetForm( @Context UriInfo ui, @QueryParam("token") String token ) {

        logger.info( "UserResource.showPasswordResetForm" );

        this.token = token;
        try {
            if ( management.checkPasswordResetTokenForAppUser( getApplicationId(), getUserUuid(), token ) ) {
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
    @Path("resetpw")
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.TEXT_HTML)
    public Viewable handlePasswordResetForm( @Context UriInfo ui, @FormParam("token") String token,
                                             @FormParam("password1") String password1,
                                             @FormParam("password2") String password2,
                                             @FormParam("recaptcha_challenge_field") String challenge,
                                             @FormParam("recaptcha_response_field") String uresponse ) {

        try {
            logger.info( "UserResource.handlePasswordResetForm" );

            this.token = token;

            if ( ( password1 != null ) || ( password2 != null ) ) {
                if ( management.checkPasswordResetTokenForAppUser( getApplicationId(), getUserUuid(), token ) ) {
                    if ( ( password1 != null ) && password1.equals( password2 ) ) {
                        management.setAppUserPassword( getApplicationId(), getUser().getUuid(), password1 );
                        management.revokeAccessTokenForAppUser( token );
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
                management.startAppUserPasswordResetFlow( getApplicationId(), getUser() );
                return handleViewable( "resetpw_email_success", this );
            }

            ReCaptchaImpl reCaptcha = new ReCaptchaImpl();
            reCaptcha.setPrivateKey( properties.getRecaptchaPrivate() );

            ReCaptchaResponse reCaptchaResponse =
                    reCaptcha.checkAnswer( httpServletRequest.getRemoteAddr(), challenge, uresponse );

            if ( reCaptchaResponse.isValid() ) {
                management.startAppUserPasswordResetFlow( getApplicationId(), getUser() );
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


    @GET
    @Path("activate")
    @Produces(MediaType.TEXT_HTML)
    public Viewable activate( @Context UriInfo ui, @QueryParam("token") String token ) {

        try {
            management.handleActivationTokenForAppUser( getApplicationId(), getUserUuid(), token );
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
            ActivationState state =
                    management.handleConfirmationTokenForAppUser( getApplicationId(), getUserUuid(), token );
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

        logger.info( "Send activation email for user: {}",  getUserUuid() );

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

        logger.info( "Revoking user tokens for {}" , getUserUuid() );

        ApiResponse response = createApiResponse();

        management.revokeAccessTokensForAppUser( getApplicationId(), getUserUuid() );

        response.setAction( "revoked user tokens" );
        return response;
    }


    @PUT
    @Path("revoketokens")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse revokeTokensPut( @Context UriInfo ui,
                                            @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {
        return revokeTokensPost( ui, callback );
    }


    @POST
    @Path("revoketoken")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse revokeTokenPost( @Context UriInfo ui,
                                            @QueryParam("callback") @DefaultValue("callback") String callback,
                                            @QueryParam("token") String token ) throws Exception {

        logger.info( "Revoking user token for {}",  getUserUuid() );

        ApiResponse response = createApiResponse();

        management.revokeAccessTokenForAppUser( token );

        response.setAction( "revoked user token" );
        return response;
    }


    @PUT
    @Path("revoketoken")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse revokeTokenPut( @Context UriInfo ui,
                                           @QueryParam("callback") @DefaultValue("callback") String callback,
                                           @QueryParam("token") String token ) throws Exception {
        return revokeTokenPost( ui, callback, token );
    }


    @GET
    @Path("token")
    @RequireApplicationAccess
    public Response getAccessToken( @Context UriInfo ui, @QueryParam("ttl") long ttl,
                                    @QueryParam("callback") @DefaultValue("") String callback ) throws Exception {

        logger.debug( "UserResource.getAccessToken" );

        try {

            if ( isApplicationUser() && !getUserUuid().equals( getSubjectUserId() ) ) {
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
        }
        if ( extensionResource != null ) {
            return extensionResource;
        }

        return super.addNameParameter( ui, itemName );
    }
}
