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
package org.apache.usergrid.rest.applications;


import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import org.apache.amber.oauth2.common.error.OAuthError;
import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.common.message.OAuthResponse;
import org.apache.amber.oauth2.common.message.types.GrantType;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.codec.Base64;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.exceptions.DisabledAdminUserException;
import org.apache.usergrid.management.exceptions.DisabledAppUserException;
import org.apache.usergrid.management.exceptions.UnactivatedAdminUserException;
import org.apache.usergrid.management.exceptions.UnactivatedAppUserException;
import org.apache.usergrid.mq.QueueManager;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.exceptions.EntityNotFoundException;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.applications.assets.AssetsResource;
import org.apache.usergrid.rest.applications.events.EventsResource;
import org.apache.usergrid.rest.applications.queues.QueueResource;
import org.apache.usergrid.rest.applications.users.UsersResource;
import org.apache.usergrid.rest.exceptions.AuthErrorInfo;
import org.apache.usergrid.rest.exceptions.NotFoundExceptionMapper;
import org.apache.usergrid.rest.exceptions.RedirectionException;
import org.apache.usergrid.rest.security.annotations.RequireApplicationAccess;
import org.apache.usergrid.rest.security.annotations.RequireOrganizationAccess;
import org.apache.usergrid.security.oauth.AccessInfo;
import org.apache.usergrid.security.oauth.ClientCredentialsInfo;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.UUID;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.usergrid.rest.exceptions.SecurityException.mappableSecurityException;
import static org.apache.usergrid.security.oauth.ClientCredentialsInfo.getUUIDFromClientId;
import static org.apache.usergrid.security.shiro.utils.SubjectUtils.isApplicationAdmin;
import static org.apache.usergrid.services.ServiceParameter.addParameter;
import static org.apache.usergrid.utils.StringUtils.stringOrSubstringAfterFirst;
import static org.apache.usergrid.utils.StringUtils.stringOrSubstringBeforeFirst;


@Component("org.apache.usergrid.rest.applications.ApplicationResource")
@Scope("prototype")
@Produces({
        MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
        "application/ecmascript", "text/jscript"
})
public class ApplicationResource extends ServiceResource {

    public static final Logger logger = LoggerFactory.getLogger( ApplicationResource.class );

    UUID applicationId;
    QueueManager queues;


    public ApplicationResource() {
    }


    public ApplicationResource init( UUID applicationId ) throws Exception {
        this.applicationId = applicationId;
        services = smf.getServiceManager( applicationId );
        queues = qmf.getQueueManager( applicationId );
        return this;
    }


    public QueueManager getQueues() {
        return queues;
    }


    @Override
    public UUID getApplicationId() {
        return applicationId;
    }


    @Path("auth")
    public AuthResource getAuthResource() throws Exception {
        return getSubResource( AuthResource.class );
    }


    @RequireApplicationAccess
    @Path("queues")
    public QueueResource getQueueResource() throws Exception {
        return getSubResource( QueueResource.class ).init( queues, "" );
    }


    @RequireApplicationAccess
    @Path("events")
    public EventsResource getEventsResource( @Context UriInfo ui ) throws Exception {
        addParameter( getServiceParameters(), "events" );

        PathSegment ps = getFirstPathSegment( "events" );
        if ( ps != null ) {
            addMatrixParams( getServiceParameters(), ui, ps );
        }

        return getSubResource( EventsResource.class );
    }


    @RequireApplicationAccess
    @Path("event")
    public EventsResource getEventResource( @Context UriInfo ui ) throws Exception {
        return getEventsResource( ui );
    }


    @RequireApplicationAccess
    @Path("assets")
    public AssetsResource getAssetsResource( @Context UriInfo ui ) throws Exception {
        logger.debug( "in assets n applicationResource" );
        addParameter( getServiceParameters(), "assets" );

        PathSegment ps = getFirstPathSegment( "assets" );
        if ( ps != null ) {
            addMatrixParams( getServiceParameters(), ui, ps );
        }

        return getSubResource( AssetsResource.class );
    }


    @RequireApplicationAccess
    @Path("asset")
    public AssetsResource getAssetResource( @Context UriInfo ui ) throws Exception {
        // TODO change to singular
        logger.debug( "in asset in applicationResource" );
        return getAssetsResource( ui );
    }


    @Path("users")
    public UsersResource getUsers( @Context UriInfo ui ) throws Exception {
        logger.debug( "ApplicationResource.getUsers" );
        addParameter( getServiceParameters(), "users" );

        PathSegment ps = getFirstPathSegment( "users" );
        if ( ps != null ) {
            addMatrixParams( getServiceParameters(), ui, ps );
        }

        return getSubResource( UsersResource.class );
    }


    @Path("user")
    public UsersResource getUsers2( @Context UriInfo ui ) throws Exception {
        return getUsers( ui );
    }


    @GET
    @Path("token")
    public Response getAccessToken( @Context UriInfo ui, @HeaderParam("Authorization") String authorization,
                                    @QueryParam("grant_type") String grant_type,
                                    @QueryParam("username") String username, @QueryParam("password") String password,
                                    @QueryParam("pin") String pin, @QueryParam("client_id") String client_id,
                                    @QueryParam("client_secret") String client_secret, @QueryParam("code") String code,
                                    @QueryParam("ttl") long ttl, @QueryParam("redirect_uri") String redirect_uri,
                                    @QueryParam("callback") @DefaultValue("") String callback ) throws Exception {

        logger.debug( "ApplicationResource.getAccessToken" );

        User user = null;

        try {

            if ( authorization != null ) {
                String type = stringOrSubstringBeforeFirst( authorization, ' ' ).toUpperCase();
                if ( "BASIC".equals( type ) ) {
                    String token = stringOrSubstringAfterFirst( authorization, ' ' );
                    String[] values = Base64.decodeToString( token ).split( ":" );
                    if ( values.length >= 2 ) {
                        client_id = values[0];
                        client_secret = values[1];
                    }
                }
            }

            // do checking for different grant types
            String errorDescription = "invalid username or password";
            if ( GrantType.PASSWORD.toString().equals( grant_type ) ) {
                try {
                    user = management
                            .verifyAppUserPasswordCredentials( services.getApplicationId(), username, password );
                }
                catch ( UnactivatedAppUserException uaue ) {
                    errorDescription = "user not activated";
                }
                catch ( DisabledAppUserException daue ) {
                    errorDescription = "user disabled";
                }
                catch ( Exception e1 ) {
                    logger.warn( "Unexpected exception during token username/password verification", e1 );

                }
            }
            else if ( "pin".equals( grant_type ) ) {
                try {
                    user = management.verifyAppUserPinCredentials( services.getApplicationId(), username, pin );
                }
                catch ( Exception e1 ) {
                    logger.warn( "Unexpected exception during token pin verification", e1 );

                }
            }
            else if ( "client_credentials".equals( grant_type ) ) {
                try {
                    AccessInfo access_info = management.authorizeClient( client_id, client_secret, ttl );
                    if ( access_info != null ) {
                        return Response.status( SC_OK ).type( jsonMediaType( callback ) )
                                       .entity( wrapWithCallback( access_info, callback ) ).build();
                    }
                }
                catch ( Exception e1 ) {
                    logger.warn( "Unexpected exception during token client authentication", e1 );
                }
            }

            if ( user == null ) {
                logger.debug("Returning 400 bad request due to: " + errorDescription );
                OAuthResponse response =
                        OAuthResponse.errorResponse( SC_BAD_REQUEST ).setError( OAuthError.TokenResponse.INVALID_GRANT )
                                     .setErrorDescription( errorDescription ).buildJSONMessage();
                return Response.status( response.getResponseStatus() ).type( jsonMediaType( callback ) )
                               .entity( wrapWithCallback( response.getBody(), callback ) ).build();
            }

            String token = management.getAccessTokenForAppUser( services.getApplicationId(), user.getUuid(), ttl );

            AccessInfo access_info =
                    new AccessInfo().withExpiresIn( tokens.getMaxTokenAgeInSeconds( token ) ).withAccessToken( token )
                                    .withProperty( "user", user );

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


    @POST
    @Path("token")
    @Consumes(APPLICATION_FORM_URLENCODED)
    public Response getAccessTokenPost( @Context UriInfo ui, @HeaderParam("Authorization") String authorization,
                                        @FormParam("grant_type") String grant_type,
                                        @FormParam("username") String username, @FormParam("password") String password,
                                        @FormParam("pin") String pin, @FormParam("client_id") String client_id,
                                        @FormParam("client_secret") String client_secret,
                                        @FormParam("code") String code, @FormParam("ttl") long ttl,
                                        @FormParam("redirect_uri") String redirect_uri,
                                        @QueryParam("callback") @DefaultValue("") String callback ) throws Exception {

        logger.debug( "ApplicationResource.getAccessTokenPost" );

        return getAccessToken( ui, authorization, grant_type, username, password, pin, client_id, client_secret, code,
                ttl, redirect_uri, callback );
    }


    @POST
    @Path("token")
    @Consumes(APPLICATION_JSON)
    public Response getAccessTokenPostJson( @Context UriInfo ui,
            @HeaderParam("Authorization") String authorization,
            Map<String, Object> json,
            @QueryParam("callback") @DefaultValue("") String callback ) throws Exception {

        String grant_type = ( String ) json.get( "grant_type" );
        String username = ( String ) json.get( "username" );
        String password = ( String ) json.get( "password" );
        String client_id = ( String ) json.get( "client_id" );
        String client_secret = ( String ) json.get( "client_secret" );
        String pin = ( String ) json.get( "pin" );
        String code = ( String ) json.get( "code" );
        String redirect_uri = ( String ) json.get( "redirect_uri" );
        long ttl = 0;

        if ( json.get( "ttl" ) != null ) {
            try {
                ttl = Long.parseLong( json.get( "ttl" ).toString() );
            }
            catch ( NumberFormatException nfe ) {
                throw new IllegalArgumentException( "ttl must be a number >= 0" );
            }
        }

        return getAccessToken( ui, authorization, grant_type, username, password, pin, client_id,
                client_secret, code, ttl, redirect_uri, callback );
    }


    @GET
    @Path("credentials")
    @RequireApplicationAccess
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse getKeys( @Context UriInfo ui,
                                    @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        logger.debug( "AuthResource.keys" );

        if ( !isApplicationAdmin( Identifier.fromUUID( applicationId ) ) ) {
            throw new UnauthorizedException();
        }

        ClientCredentialsInfo kp =
                new ClientCredentialsInfo( management.getClientIdForApplication( services.getApplicationId() ),
                        management.getClientSecretForApplication( services.getApplicationId() ) );

        return   createApiResponse().withCredentials( kp ).withAction( "get application keys" ).withSuccess();
    }


    @POST
    @Path("credentials")
    @RequireApplicationAccess
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse generateKeys( @Context UriInfo ui,
        @QueryParam("callback") @DefaultValue("callback") String callback ) throws Exception {

        logger.debug( "AuthResource.keys" );

        if ( !isApplicationAdmin( Identifier.fromUUID( applicationId ) ) ) {
            throw new UnauthorizedException();
        }

        ClientCredentialsInfo kp = new ClientCredentialsInfo(
            management.getClientIdForApplication( services.getApplicationId() ),
            management.newClientSecretForApplication( services.getApplicationId() ) );

        return createApiResponse().withCredentials( kp ).withAction( "generate application keys" ).withSuccess();
    }


    @GET
    @Path("authorize")
    public Viewable showAuthorizeForm(
            @Context UriInfo ui,
            @QueryParam("response_type") String response_type,
            @QueryParam("client_id") String client_id,
            @QueryParam("redirect_uri") String redirect_uri,
            @QueryParam("scope") String scope,
            @QueryParam("state") String state ) {

        try {
            UUID uuid = getUUIDFromClientId( client_id );
            if ( uuid == null ) {
                throw mappableSecurityException( AuthErrorInfo.OAUTH2_INVALID_CLIENT,
                        "Unable to authenticate (OAuth). Invalid client_id" );
            }

            responseType = response_type;
            clientId = client_id;
            redirectUri = redirect_uri;
            this.scope = scope;
            this.state = state;

            ApplicationInfo app = management.getApplicationInfo( applicationId );
            applicationName = app.getName();

            return handleViewable( "authorize_form", this );
        }
        catch ( RedirectionException e ) {
            throw e;
        }
        catch ( Exception e ) {
            return handleViewable( "error", e );
        }
    }


    @POST
    @Path("authorize")
    @Produces(MediaType.TEXT_HTML)
    public Response handleAuthorizeForm( @Context UriInfo ui,
            @FormParam("response_type") String response_type,
            @FormParam("client_id") String client_id,
            @FormParam("redirect_uri") String redirect_uri,
            @FormParam("scope") String scope, @FormParam("state") String state,
            @FormParam("username") String username,
            @FormParam("password") String password ) {

        logger.debug( "ApplicationResource /authorize: {}/{}", username, password );

        try {
            responseType = response_type;
            clientId = client_id;
            redirectUri = redirect_uri;
            this.scope = scope;
            this.state = state;

            User user = null;
            String errorDescription = "Username or password do not match";
            try {
                user = management.verifyAppUserPasswordCredentials( services.getApplicationId(), username, password );
            }
            catch ( UnactivatedAdminUserException uaue ) {
                errorDescription = "user not activated";
            }
            catch ( DisabledAdminUserException daue ) {
                errorDescription = "user disabled";
            }
            catch ( Exception e1 ) {
                logger.warn("Unexpected exception in authorize username/password verification", e1);
            }

            if ( ( user != null ) && isNotBlank( redirect_uri ) ) {
                String authorizationCode =
                        management.getAccessTokenForAppUser( services.getApplicationId(), user.getUuid(), 0 );
                redirect_uri = buildRedirectUriWithAuthCode( redirect_uri, state, authorizationCode );

                throw new RedirectionException( redirect_uri );
            }
            else {
                errorMsg = errorDescription;
            }

            ApplicationInfo app = management.getApplicationInfo( applicationId );
            applicationName = app.getName();

            return Response.ok( handleViewable( "authorize_form", this ) ).build() ;
        }
        catch ( RedirectionException e ) {
            throw e;
        }
        catch ( Exception e ) {
            logger.debug("handleAuthorizeForm failed", e);
            return Response.ok( handleViewable( "error", this ) ).build() ;
        }
    }


    @Override
    @DELETE
    @RequireOrganizationAccess
    public ApiResponse executeDelete( @Context final UriInfo ui, @DefaultValue( "callback" ) final String callback,
                                          final String confirmAppDelete ) throws Exception {
        throw new UnsupportedOperationException( "Delete must be done from the management endpoint" );
    }


    String errorMsg = "";
    String applicationName;
    String responseType;
    String clientId;
    String redirectUri;
    String scope;
    String state;


    public String getErrorMsg() {
        return errorMsg;
    }


    public String getApplicationName() {
        return applicationName;
    }


    public String getResponseType() {
        return responseType;
    }


    public String getClientId() {
        return clientId;
    }


    public String getRedirectUri() {
        return redirectUri;
    }


    public String getScope() {
        return scope;
    }


    public String getState() {
        return state;
    }


    // todo: find a mechanism to move these methods into the push notifications
    // project
    @RequireApplicationAccess
    @Path("notifiers")
    public AbstractContextResource getNotifiersResource( @Context UriInfo ui ) throws Exception {

        Class cls = Class.forName( "org.apache.usergrid.rest.applications.notifiers.NotifiersResource" );

        logger.debug( "NotifiersResource.getNotifiersResource" );
        addParameter( getServiceParameters(), "notifiers" );

        PathSegment ps = getFirstPathSegment( "notifiers" );
        if ( ps != null ) {
            addMatrixParams( getServiceParameters(), ui, ps );
        }

        return getSubResource( cls );
    }


    @RequireApplicationAccess
    @Path("notifier")
    public AbstractContextResource getNotifierResource( @Context UriInfo ui ) throws Exception {
        return getNotifiersResource( ui );
    }


    private String buildRedirectUriWithAuthCode( String redirect_uri, String state, String authorizationCode )
            throws UnsupportedEncodingException {
        if ( !redirect_uri.contains( "?" ) ) {
            redirect_uri += "?";
        }
        else {
            redirect_uri += "&";
        }
        redirect_uri += "code=" + authorizationCode;

        if ( isNotBlank( state ) ) {
            redirect_uri += "&state=" + URLEncoder.encode( state, "UTF-8" );
        }

        return redirect_uri;
    }


    //mAX /APM integration
    public static final String APIGEE_MOBILE_APM_CONFIG_JSON_KEY = "apigeeMobileConfig";


    @GET
    @Path("apm/apigeeMobileConfig")
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Object getAPMConfig( @Context UriInfo ui,
                                         @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {
        EntityManager em = emf.getEntityManager( applicationId );
        Object value = em.getProperty( new SimpleEntityRef( Application.ENTITY_TYPE, applicationId ),
                APIGEE_MOBILE_APM_CONFIG_JSON_KEY );
        //If there is no apm configuration then try to create apm config on the fly
        if ( value == null ) {
            value = management.registerAppWithAPM(
                management.getOrganizationForApplication( applicationId ),
                management.getApplicationInfo( applicationId )
            );
        }
        if(value==null){
            throw new EntityNotFoundException("apigeeMobileConfig not found, it is possibly not enabled for your config.");
        }
        return value;
    }
}
