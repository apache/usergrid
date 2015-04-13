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


import java.net.URLEncoder;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.management.ApplicationCreator;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.OrganizationOwnerInfo;
import org.apache.usergrid.persistence.exceptions.EntityNotFoundException;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import org.apache.amber.oauth2.common.error.OAuthError;
import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.common.message.OAuthResponse;
import org.apache.amber.oauth2.common.message.types.GrantType;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.codec.Base64;

import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.management.exceptions.DisabledAdminUserException;
import org.apache.usergrid.management.exceptions.UnactivatedAdminUserException;
import org.apache.usergrid.management.exceptions.UnconfirmedAdminUserException;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.exceptions.RedirectionException;
import org.apache.usergrid.rest.management.organizations.OrganizationsResource;
import org.apache.usergrid.rest.management.users.UsersResource;
import org.apache.usergrid.security.oauth.AccessInfo;
import org.apache.usergrid.security.shiro.utils.SubjectUtils;

import com.sun.jersey.api.view.Viewable;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import static org.apache.commons.lang.StringUtils.isNotBlank;
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

    /*-
     * New endpoints:
     * 
     * /management/organizations/<organization-name>/applications
     * /management/organizations/<organization-name>/users
     * /management/organizations/<organization-name>/keys
     *
     * /management/users/<user-name>/login
     * /management/users/<user-name>/password
     * 
     */

    private static final Logger logger = LoggerFactory.getLogger( ManagementResource.class );

    @Autowired
    private ApplicationCreator applicationCreator;

    public static final String USERGRID_CENTRAL_URL = "usergrid.central.url";


    public ManagementResource() {
        logger.info( "ManagementResource initialized" );
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
        return getAccessTokenInternal( ui, authorization, grant_type, username, password, client_id, client_secret, ttl,
                callback, false );
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
                callback, true );
    }


    private Response getAccessTokenInternal( UriInfo ui, String authorization, String grant_type, String username,
                                             String password, String client_id, String client_secret, long ttl,
                                             String callback, boolean loadAdminData ) throws Exception {
        UserInfo user = null;

        try {
            if ( SubjectUtils.getUser() != null ) {
                user = SubjectUtils.getUser();
            }

            logger.info( "ManagementResource.getAccessToken with username: {}", username );

            String errorDescription = "invalid username or password";

            if ( user == null ) {
                if ( authorization != null ) {
                    String type = stringOrSubstringBeforeFirst( authorization, ' ' ).toUpperCase();

                    if ( "BASIC".equals( type ) ) {
                        String token = stringOrSubstringAfterFirst( authorization, ' ' );
                        String[] values = Base64.decodeToString( token ).split( ":" );

                        if ( values.length >= 2 ) {
                            client_id = values[0].toLowerCase();
                            client_secret = values[1];
                        }
                    }
                }


                // do checking for different grant types
                if ( GrantType.PASSWORD.toString().equals( grant_type ) ) {
                    try {
                        user = management.verifyAdminUserPasswordCredentials( username, password );

                        if ( user != null ) {
                            logger.info( "found user from verify: {}", user.getUuid() );
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
                        logger.warn( "Responding with HTTP 403 forbidden response for unconfirmed user {}" , user);

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

            access_info.setProperty( "user", management.getAdminUserOrganizationData( user, loadAdminData ) );

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

        logger.info( "ManagementResource.getAccessTokenPost" );

        return getAccessTokenInternal( ui, authorization, grant_type, username, password, client_id, client_secret, ttl,
                callback, true );
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
                callback, false );
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
                callback, true );
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
                callback, false );
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
            return handleViewable( "error", e );
        }
    }


    /**
     * <p>
     * Validates access token from other or "external" Usergrid system.
     * Calls other system's /management/me endpoint to get the User associated with the access token.
     * If user does not exist locally, then user and organization with the same name of user is created.
     * If no user is returned from the other cluster, then this endpoint will return 401.
     * <p>
     *
     * <p>
     * See <a href="https://issues.apache.org/jira/browse/USERGRID-567">USERGRID-567</a>
     * for details about Usergrid Central SSO.
     * </p>
     *
     * @param ui             Information about calling URI.
     * @param json           JSON object with fields: ext_access_token, ttl
     * @param callback       For JSONP support.
     * @return               Returns JSON object with access_token field.
     * @throws Exception     Returns 401 if access token cannot be validated
     */
    @POST
    @Path( "/externaltoken" )
    //@RequireSystemAccess TODO: should this be required
    public Response validateExternalToken(
            @Context UriInfo ui,
            Map<String, Object> json,
            @QueryParam( "callback" ) @DefaultValue( "" ) String callback )  throws Exception {

        Object extAccessTokenObj = json.get("ext_access_token");
        if ( extAccessTokenObj == null ) {
            throw new IllegalArgumentException("ext_access_token must be specified");
        }
        String extAccessToken = json.get("ext_access_token").toString();

        Object ttlObj = json.get("ttl");
        if ( ttlObj == null ) {
            throw new IllegalArgumentException("ttl must be specified");
        }
        long ttl;
        try {
            ttl = Long.parseLong(ttlObj.toString());
        } catch ( NumberFormatException e ) {
            throw new IllegalArgumentException("ttl must be specified as a long");
        }

        return validateExternalToken( ui, extAccessToken, ttl, callback );
    }


    /**
     * <p>
     * Validates access token from other or "external" Usergrid system.
     * Calls other system's /management/me endpoint to get the User associated with the access token.
     * If user does not exist locally, then user and organization with the same name of user is created.
     * If no user is returned from the other cluster, then this endpoint will return 401.
     * </p>
     *
     * <p> Part of Usergrid Central SSO feature.
     * See <a href="https://issues.apache.org/jira/browse/USERGRID-567">USERGRID-567</a>
     * for details about Usergrid Central SSO.
     * </p>
     *
     * @param ui             Information about calling URI.
     * @param extAccessToken Access token from external Usergrid system.
     * @param ttl            Time to live for token.
     * @param callback       For JSONP support.
     * @return               Returns JSON object with access_token field.
     * @throws Exception     Returns 401 if access token cannot be validated
     */
    @GET
    @Path( "/externaltoken" )
    //@RequireSystemAccess TODO: should this be required
    public Response validateExternalToken(
                                @Context UriInfo ui,
                                @QueryParam( "ext_access_token" ) String extAccessToken,
                                @QueryParam( "ttl" ) @DefaultValue("-1") long ttl,
                                @QueryParam( "callback" ) @DefaultValue( "" ) String callback )
            throws Exception {


        if ( extAccessToken == null ) {
            throw new IllegalArgumentException("ext_access_token must be specified");
        }

        if ( ttl == -1 ) {
            throw new IllegalArgumentException("ttl must be specified");
        }

        // create URL of central Usergrid's /management/me endpoint

        String externalUrl = properties.getProperty( USERGRID_CENTRAL_URL ).trim();
        // be lenient about trailing slash
        externalUrl = !externalUrl.endsWith( "/" ) ? externalUrl + "/" : externalUrl;
        String me = externalUrl + "management/me?access_token=" + extAccessToken;

        // use our favorite HTTP client to GET /management/me

        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        Client client = Client.create(clientConfig);
        final JsonNode accessInfoNode;
        try {
            accessInfoNode = client.resource( me )
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .get(JsonNode.class);

        } catch ( Exception e ) {
            // user not found 404
            String msg = "Cannot find Admin User associated with " + extAccessToken;
            throw new EntityNotFoundException( msg, e );
        }

        // good. user was found in central Usergrid

        JsonNode userNode = accessInfoNode.get( "user" );
        String username = userNode.get( "username" ).getTextValue();
        String name     = userNode.get( "name" ).getTextValue();
        String email    = userNode.get( "email" ).getTextValue();

        // set dummy password to random string that nobody can guess, in SSO setup
        // admin users should never be able to login directly to this Usergrid system
        String dummyPassword = RandomStringUtils.randomAlphanumeric( 40 );

        // if user does not exist locally then we need to fix that

        final UUID userId;

        final OrganizationInfo organizationInfo = management.getOrganizationByName(username);
        if ( organizationInfo == null ) {

            // create local user and personal organization, activate user.

            OrganizationOwnerInfo ownerOrgInfo = management.createOwnerAndOrganization(
                    username, username, name, email, dummyPassword, true, true );
            userId = ownerOrgInfo.getOwner().getUuid();

            management.activateOrganization( ownerOrgInfo.getOrganization() );

            applicationCreator.createSampleFor( ownerOrgInfo.getOrganization() );

        } else {
            userId = management.getAdminUserByUsername( username ).getUuid();
        }

        // store the external access_token as if it were one of our own
        management.importTokenForAdminUser( userId, extAccessToken, ttl );

        // success! return JSON object with access_token field
        AccessInfo accessInfo = new AccessInfo()
                .withExpiresIn( tokens.getMaxTokenAgeInSeconds(extAccessToken ) )
                .withAccessToken( extAccessToken );

        return Response.status( SC_OK ).type( jsonMediaType( callback ) ).entity( accessInfo ).build();
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
