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
package org.apache.usergrid.rest.management;


import org.apache.amber.oauth2.common.error.OAuthError;
import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.common.message.OAuthResponse;
import org.apache.amber.oauth2.common.message.types.GrantType;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.codec.Base64;
import org.apache.usergrid.management.ApplicationCreator;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.management.exceptions.DisabledAdminUserException;
import org.apache.usergrid.management.exceptions.UnactivatedAdminUserException;
import org.apache.usergrid.management.exceptions.UnconfirmedAdminUserException;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.exceptions.RedirectionException;
import org.apache.usergrid.rest.management.organizations.OrganizationsResource;
import org.apache.usergrid.rest.management.users.UsersResource;
import org.apache.usergrid.security.oauth.AccessInfo;
import org.apache.usergrid.security.shiro.utils.SubjectUtils;
import org.apache.usergrid.security.sso.ExternalSSOProvider;
import org.apache.usergrid.security.sso.SSOProviderFactory;
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
import java.net.URLEncoder;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.*;
import static javax.ws.rs.core.MediaType.*;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.usergrid.security.tokens.cassandra.TokenServiceImpl.USERGRID_EXTERNAL_SSO_ENABLED;
import static org.apache.usergrid.security.tokens.cassandra.TokenServiceImpl.USERGRID_EXTERNAL_PROVIDER_URL;
import static org.apache.usergrid.utils.JsonUtils.mapToJsonString;
import static org.apache.usergrid.utils.StringUtils.stringOrSubstringAfterFirst;
import static org.apache.usergrid.utils.StringUtils.stringOrSubstringBeforeFirst;


@Path( "/management" )
@Component
@Scope( "singleton" )
@Produces( {
        MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
        "application/ecmascript", "text/jscript"
} )
public class ManagementResource extends AbstractContextResource {

    private static final Logger logger = LoggerFactory.getLogger( ManagementResource.class );

    /*-
     * New endpoints:
     *
     * /management/externaltoken?ext_access_token=<token-from-central-usergrid>&ttl=<time-to-live>
     *
     * /management/organizations/<organization-name>/applications
     * /management/organizations/<organization-name>/users
     * /management/organizations/<organization-name>/keys
     *
     * /management/users/<user-name>/login
     * /management/users/<user-name>/password
     *
     */

    @Autowired
    private ApplicationCreator applicationCreator;

    @Autowired
    private SSOProviderFactory ssoProviderFactory;

    // usergrid configuration property names needed
    public static final String USERGRID_SYSADMIN_LOGIN_NAME = "usergrid.sysadmin.login.name";

    MetricsFactory metricsFactory = null;


    public ManagementResource() {
        if (logger.isTraceEnabled()) {
            logger.trace( "ManagementResource initialized" );
        }
    }


    MetricsFactory getMetricsFactory() {
        if ( metricsFactory == null ) {
            metricsFactory = injector.getInstance( MetricsFactory.class );
        }
        return metricsFactory;
    }


    private static String wrapWithCallback( AccessInfo accessInfo, String callback ) {
        return wrapWithCallback( mapToJsonString( accessInfo ), callback );
    }


    private static String wrapWithCallback( String json, String callback ) {
        if ( StringUtils.isNotBlank( callback ) ) {
            json = callback + "(" + json + ")";
        }
        return json;
    }


    private static MediaType jsonMediaType( String callback ) {
        return isNotBlank( callback ) ? new MediaType( "application", "javascript" ) : APPLICATION_JSON_TYPE;
    }


    @Path( "organizations" )
    public OrganizationsResource getOrganizations() {
        return getSubResource( OrganizationsResource.class );
    }


    @Path( "orgs" )
    public OrganizationsResource getOrganizations2() {
        return getSubResource( OrganizationsResource.class );
    }


    @Path( "users" )
    public UsersResource getUsers() {
        return getSubResource( UsersResource.class );
    }


    @GET
    @Path( "me" )
    public Response getAccessTokenLight( @Context UriInfo ui, @HeaderParam( "Authorization" ) String authorization,
                                         @QueryParam( "grant_type" ) String grant_type,
                                         @QueryParam( "username" ) String username,
                                         @QueryParam( "password" ) String password,
                                         @QueryParam( "client_id" ) String client_id,
                                         @QueryParam( "client_secret" ) String client_secret,
                                         @QueryParam( "ttl" ) long ttl,
                                         @QueryParam( "access_token" ) String access_token,
                                         @QueryParam( "callback" ) @DefaultValue( "" ) String callback )
            throws Exception {


        final UserInfo user = SubjectUtils.getUser();

        // if user is null ( meaning no token was provided and previously validated in OAuth2AccessTokenSecurityFilter)
        // then assume it's a token request
        if( user == null) {
            return getAccessTokenInternal(ui, authorization, grant_type, username, password, client_id, client_secret, ttl,
                callback, false, true);
        }



        // if it's not a token request and we have a user, extract details from the token

        final long passwordChanged = management.getLastAdminPasswordChange( user.getUuid() );
        final boolean ssoEnabled = Boolean.parseBoolean(properties.getProperty(USERGRID_EXTERNAL_SSO_ENABLED));
        long tokenTtl;

        if(ssoEnabled){

            ExternalSSOProvider provider = ssoProviderFactory.getProvider();

            tokenTtl =
                Long.valueOf(provider.getDecodedTokenDetails(access_token).get("expiry")) - System.currentTimeMillis()/1000;

        }else{

            tokenTtl = tokens.getTokenInfo(access_token).getDuration();
        }


        final AccessInfo access_info = new AccessInfo().withExpiresIn( tokenTtl ).withAccessToken( access_token )
            .withPasswordChanged( passwordChanged );

        access_info.setProperty( "user", management.getAdminUserOrganizationData( user, true ) );

        return Response.status( SC_OK ).type( jsonMediaType( callback ) )
            .entity( wrapWithCallback( access_info, callback ) ).build();


    }


    @GET
    @Path( "token" )
    public Response getAccessToken( @Context UriInfo ui, @HeaderParam( "Authorization" ) String authorization,
                                    @QueryParam( "grant_type" ) String grant_type,
                                    @QueryParam( "username" ) String username,
                                    @QueryParam( "password" ) String password,
                                    @QueryParam( "client_id" ) String client_id,
                                    @QueryParam( "client_secret" ) String client_secret, @QueryParam( "ttl" ) long ttl,
                                    @QueryParam( "callback" ) @DefaultValue( "" ) String callback ) throws Exception {
        return getAccessTokenInternal( ui, authorization, grant_type, username, password, client_id, client_secret, ttl,
                callback, false, false);
    }


    private Response getAccessTokenInternal(UriInfo ui, String authorization, String grant_type, String username,
                                           String password, String client_id, String client_secret, long ttl,
                                           String callback, boolean adminData, boolean me) throws Exception {




        UserInfo user = null;

        try {
            if ( SubjectUtils.getUser() != null ) {
                user = SubjectUtils.getUser();
            }

            if (logger.isTraceEnabled()) {
                logger.trace("ManagementResource.getAccessToken with username: {}", username);
            }

            String errorDescription = "invalid username or password";

            if ( user == null ) {



                //if ( !me ) { // if not lightweight-auth, i.e. /management/me then...

                    // make sure authentication is allowed considering
                    // external token validation configuration (UG Central SSO)
                    ensureAuthenticationAllowed( username, grant_type );
               // }

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
                if ( GrantType.PASSWORD.toString().equals( grant_type ) ) {
                    try {
                        user = management.verifyAdminUserPasswordCredentials( username, password );

                        if ( user != null ) {
                            if (logger.isTraceEnabled()) {
                                logger.trace("found user from verify: {}", user.getUuid());
                            }
                        }
                    }
                    catch ( UnactivatedAdminUserException uaue ) {
                        errorDescription = "user not activated";
                        logger.error( errorDescription, uaue );
                    }
                    catch ( DisabledAdminUserException daue ) {
                        errorDescription = "user disabled";
                        logger.error( errorDescription, daue );
                    }
                    catch ( UnconfirmedAdminUserException uaue ) {
                        errorDescription = "User must be confirmed to authenticate";
                        logger.warn("Responding with HTTP 403 forbidden response for unconfirmed user");

                        OAuthResponse response = OAuthResponse.errorResponse( SC_FORBIDDEN )
                                                              .setError( OAuthError.TokenResponse.INVALID_GRANT )
                                                              .setErrorDescription( errorDescription )
                                                              .buildJSONMessage();

                        return Response.status( response.getResponseStatus() ).type( jsonMediaType( callback ) )
                                       .entity( wrapWithCallback( response.getBody(), callback ) ).build();
                    }
                    catch ( Exception e1 ) {
                        logger.error( errorDescription, e1 );
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
                        logger.error( "failed authorizeClient", e1 );
                    }
                }
            }

            if ( user == null ) {
                //TODO: this could be fixed to return the reason why a user is null. In some cases the USER is not found
                //so a 404 would be more appropriate etc...
                OAuthResponse response =
                        OAuthResponse.errorResponse( SC_BAD_REQUEST ).setError( OAuthError.TokenResponse.INVALID_GRANT )
                                     .setErrorDescription( errorDescription ).buildJSONMessage();
                return Response.status( response.getResponseStatus() ).type( jsonMediaType( callback ) )
                               .entity( wrapWithCallback( response.getBody(), callback ) ).build();
            }

            String token = management.getAccessTokenForAdminUser( user.getUuid(), ttl );
            Long passwordChanged = management.getLastAdminPasswordChange( user.getUuid() );

            AccessInfo access_info =
                    new AccessInfo().withExpiresIn( tokens.getMaxTokenAgeInSeconds( token ) ).withAccessToken( token )
                                    .withPasswordChanged( passwordChanged );

            access_info.setProperty( "user", management.getAdminUserOrganizationData( user, me ) );

            // increment counters for admin login
            management.countAdminUserAction( user, "login" );

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
    @Path( "token" )
    @Consumes( APPLICATION_FORM_URLENCODED )
    public Response getAccessTokenPost( @Context UriInfo ui, @FormParam( "grant_type" ) String grant_type,
                                        @HeaderParam( "Authorization" ) String authorization,
                                        @FormParam( "username" ) String username,
                                        @FormParam( "password" ) String password,
                                        @FormParam( "client_id" ) String client_id, @FormParam( "ttl" ) long ttl,
                                        @FormParam( "client_secret" ) String client_secret,
                                        @QueryParam( "callback" ) @DefaultValue( "" ) String callback )
            throws Exception {

        if (logger.isTraceEnabled()) {
            logger.trace("ManagementResource.getAccessTokenPost");
        }

        return getAccessTokenInternal( ui, authorization, grant_type, username, password, client_id, client_secret, ttl,
                callback, false, false);
    }


    @POST
    @Path( "me" )
    @Consumes( APPLICATION_FORM_URLENCODED )
    public Response getAccessTokenLightPost( @Context UriInfo ui, @HeaderParam( "Authorization" ) String authorization,
                                             @FormParam( "grant_type" ) String grant_type,
                                             @FormParam( "username" ) String username,
                                             @FormParam( "password" ) String password,
                                             @FormParam( "client_id" ) String client_id,
                                             @FormParam( "client_secret" ) String client_secret,
                                             @FormParam( "ttl" ) long ttl,
                                             @FormParam( "access_token" ) String access_token,
                                             @FormParam( "callback" ) @DefaultValue( "" ) String callback )
            throws Exception {
        return getAccessTokenInternal( ui, authorization, grant_type, username, password, client_id, client_secret, ttl,
                callback, false, true );
    }


    @POST
    @Path( "token" )
    @Consumes( APPLICATION_JSON )
    public Response getAccessTokenPostJson( @Context UriInfo ui, @HeaderParam( "Authorization" ) String authorization,
                                            Map<String, Object> json,
                                            @QueryParam( "callback" ) @DefaultValue( "" ) String callback )
            throws Exception {

        String grant_type = ( String ) json.get( "grant_type" );
        String username = ( String ) json.get( "username" );
        String password = ( String ) json.get( "password" );
        String client_id = ( String ) json.get( "client_id" );
        String client_secret = ( String ) json.get( "client_secret" );
        long ttl = 0;

        if ( json.get( "ttl" ) != null ) {
            try {
                ttl = Long.parseLong( json.get( "ttl" ).toString() );
            }
            catch ( NumberFormatException nfe ) {
                throw new IllegalArgumentException( "ttl must be a number >= 0" );
            }
        }

        return getAccessTokenInternal( ui, authorization, grant_type, username, password, client_id, client_secret, ttl,
                callback, false, false );
    }


    @POST
    @Path( "me" )
    @Consumes( APPLICATION_JSON )
    public Response getAccessTokenMePostJson( @Context UriInfo ui, Map<String, Object> json,
                                              @QueryParam( "callback" ) @DefaultValue( "" ) String callback,
                                              @HeaderParam( "Authorization" ) String authorization ) throws Exception {

        String grant_type = ( String ) json.get( "grant_type" );
        String username = ( String ) json.get( "username" );
        String password = ( String ) json.get( "password" );
        String client_id = ( String ) json.get( "client_id" );
        String client_secret = ( String ) json.get( "client_secret" );
        long ttl = 0;

        if ( json.get( "ttl" ) != null ) {
            try {
                ttl = Long.parseLong( json.get( "ttl" ).toString() );
            }
            catch ( NumberFormatException nfe ) {
                throw new IllegalArgumentException( "ttl must be a number >= 0" );
            }
        }

        return getAccessTokenInternal( ui, authorization, grant_type, username, password, client_id, client_secret, ttl,
                callback, false, false );
    }


    @GET
    @Path( "authorize" )
    @Produces( MediaType.TEXT_HTML )
    public Viewable showAuthorizeForm( @Context UriInfo ui, @QueryParam( "response_type" ) String response_type,
                                       @QueryParam( "client_id" ) String client_id,
                                       @QueryParam( "redirect_uri" ) String redirect_uri,
                                       @QueryParam( "scope" ) String scope, @QueryParam( "state" ) String state ) {

        responseType = response_type;
        clientId = client_id;
        redirectUri = redirect_uri;
        this.scope = scope;
        this.state = state;

        return handleViewable( "authorize_form", this );
    }


    @POST
    @Path( "authorize" )
    @Produces( MediaType.TEXT_HTML )
    public Viewable handleAuthorizeForm( @Context UriInfo ui, @FormParam( "response_type" ) String response_type,
                                         @FormParam( "client_id" ) String client_id,
                                         @FormParam( "redirect_uri" ) String redirect_uri,
                                         @FormParam( "scope" ) String scope, @FormParam( "state" ) String state,
                                         @FormParam( "username" ) String username,
                                         @FormParam( "password" ) String password ) {

        if (logger.isTraceEnabled()) {
            logger.trace("ManagementResource /authorize: {}", username);
        }

       try {
            responseType = response_type;
            clientId = client_id;
            redirectUri = redirect_uri;
            this.scope = scope;
            this.state = state;

            UserInfo user = null;
            try {
                user = management.verifyAdminUserPasswordCredentials( username, password );
            }
            catch ( Exception e1 ) {
                // intentionally empty
            }
            if ( ( user != null ) && isNotBlank( redirect_uri ) ) {
                if ( !redirect_uri.contains( "?" ) ) {
                    redirect_uri += "?";
                }
                else {
                    redirect_uri += "&";
                }
                redirect_uri += "code=" + management.getAccessTokenForAdminUser( user.getUuid(), 0 );
                if ( isNotBlank( state ) ) {
                    redirect_uri += "&state=" + URLEncoder.encode( state, "UTF-8" );
                }
                throw new RedirectionException( state );
            }
            else {
                errorMsg = "Username or password do not match";
            }

            return handleViewable( "authorize_form", this );
        }
        catch ( RedirectionException e ) {
            throw e;
        }
        catch ( Exception e ) {
            logger.error("handleAuthorizeForm failed", e);
            return handleViewable( "error", e );
        }
    }


    /**
     * Check that authentication is allowed. If external token validation is enabled (Central Usergrid SSO)
     * then only superusers should be allowed to login directly to this Usergrid instance.
     */
    private void ensureAuthenticationAllowed( String username, String grant_type ) {

        if ( username == null || grant_type == null || !grant_type.equalsIgnoreCase( "password" )) {
            return; // we only care about username/password auth
        }

        //why !isexternal_sso_enabled ?
//        final boolean externalTokensEnabled =
//                !StringUtils.isEmpty( properties.getProperty( USERGRID_EXTERNAL_SSO_ENABLED ) );

        if ( tokens.isExternalSSOProviderEnabled() ) {

            // when external tokens enabled then only superuser can obtain an access token

            final String superuserName = properties.getProperty( USERGRID_SYSADMIN_LOGIN_NAME );
            if ( !username.equalsIgnoreCase( superuserName )) {

                // this guy is not the superuser
                throw new IllegalArgumentException( "Admin Users must login via " +
                        properties.getProperty( USERGRID_EXTERNAL_PROVIDER_URL ) );
            }
        }
    }


    String errorMsg = "";
    String responseType;
    String clientId;
    String redirectUri;
    String scope;
    String state;


    public String getErrorMsg() {
        return errorMsg;
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
}
