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


import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Injector;
import org.apache.amber.oauth2.common.error.OAuthError;
import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.common.message.OAuthResponse;
import org.apache.amber.oauth2.common.message.types.GrantType;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.shiro.codec.Base64;
import org.apache.usergrid.exception.NotImplementedException;
import org.apache.usergrid.management.ApplicationCreator;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.OrganizationOwnerInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.management.exceptions.DisabledAdminUserException;
import org.apache.usergrid.management.exceptions.UnactivatedAdminUserException;
import org.apache.usergrid.management.exceptions.UnconfirmedAdminUserException;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.exceptions.EntityNotFoundException;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.exceptions.RedirectionException;
import org.apache.usergrid.rest.management.organizations.OrganizationsResource;
import org.apache.usergrid.rest.management.users.UsersResource;
import org.apache.usergrid.security.oauth.AccessInfo;
import org.apache.usergrid.security.shiro.utils.SubjectUtils;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import static javax.servlet.http.HttpServletResponse.*;
import static javax.ws.rs.core.MediaType.*;
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
    Injector injector;


    private static Client jerseyClient = null;


    // names for metrics to be collected
    private static final String SSO_TOKENS_REJECTED = "sso.tokens_rejected";
    private static final String SSO_TOKENS_VALIDATED = "sso.tokens_validated";
    private static final String SSO_CREATED_LOCAL_ADMINS = "sso.created_local_admins";
    private static final String SSO_PROCESSING_TIME = "sso.processing_time";

    // usergrid configuration property names needed
    public static final String USERGRID_SYSADMIN_LOGIN_NAME = "usergrid.sysadmin.login.name";
    public static final String USERGRID_CENTRAL_URL =         "usergrid.central.url";
    public static final String CENTRAL_CONNECTION_POOL_SIZE = "usergrid.central.connection.pool.size";
    public static final String CENTRAL_CONNECTION_TIMEOUT =   "usergrid.central.connection.timeout";
    public static final String CENTRAL_READ_TIMEOUT =         "usergrid.central.read.timeout";

    MetricsFactory metricsFactory = null;


    public ManagementResource() {
        logger.info( "ManagementResource initialized" );
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
        return getAccessTokenInternal( ui, authorization, grant_type, username, password, client_id, client_secret, ttl,
                callback, false, true );
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

            logger.info( "ManagementResource.getAccessToken with username: {}", username );

            String errorDescription = "invalid username or password";

            if ( user == null ) {

                if ( !me ) { // if not lightweight-auth, i.e. /management/me then...

                    // make sure authentication is allowed considering
                    // external token validation configuration (UG Central SSO)
                    ensureAuthenticationAllowed( username, grant_type );
                }

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

        logger.info( "ManagementResource.getAccessTokenPost" );

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

       logger.debug( "ManagementResource /authorize: {}/{}", username, password );

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
            logger.debug("handleAuthorizeForm failed", e);
            return handleViewable( "error", e );
        }
    }


    /**
     * <p>
     * Allows call to validateExternalToken() (see below) with a POST of a JSON object.
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
    public Response validateExternalToken(
            @Context UriInfo ui,
            Map<String, Object> json,
            @QueryParam( "callback" ) @DefaultValue( "" ) String callback )  throws Exception {

        if ( StringUtils.isEmpty( properties.getProperty( USERGRID_CENTRAL_URL ))) {
            throw new NotImplementedException( "External Token Validation Service is not configured" );
        }

        Object extAccessTokenObj = json.get( "ext_access_token" );
        if ( extAccessTokenObj == null ) {
            throw new IllegalArgumentException("ext_access_token must be specified");
        }
        String extAccessToken = json.get("ext_access_token").toString();

        Object ttlObj = json.get( "ttl" );
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
     * Calls other system's /management/me endpoint to get the User
     * associated with the access token. If user does not exist locally,
     * then user and organizations will be created. If no user is returned
     * from the other cluster, then this endpoint will return 401.
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
    public Response validateExternalToken(
                                @Context UriInfo ui,
                                @QueryParam( "ext_access_token" ) String extAccessToken,
                                @QueryParam( "ttl" ) @DefaultValue("-1") long ttl,
                                @QueryParam( "callback" ) @DefaultValue( "" ) String callback )
            throws Exception {


        if ( StringUtils.isEmpty( properties.getProperty( USERGRID_CENTRAL_URL ))) {
            throw new NotImplementedException( "External Token Validation Service is not configured" );
        }

        if ( extAccessToken == null ) {
            throw new IllegalArgumentException("ext_access_token must be specified");
        }

        if ( ttl == -1 ) {
            throw new IllegalArgumentException("ttl must be specified");
        }
        AccessInfo accessInfo = null;

        Timer processingTimer = getMetricsFactory().getTimer(
            ManagementResource.class, SSO_PROCESSING_TIME );

        Timer.Context timerContext = processingTimer.time();

        try {
            // look up user via UG Central's /management/me endpoint.

            JsonNode accessInfoNode = getMeFromUgCentral( extAccessToken );

            JsonNode userNode = accessInfoNode.get( "user" );
            String username = userNode.get( "username" ).textValue();

            // if user does not exist locally then we need to fix that

            UserInfo userInfo = management.getAdminUserByUsername( username );
            UUID userId = userInfo == null ? null : userInfo.getUuid();

            if ( userId == null ) {

                // create local user and and organizations they have on the central Usergrid instance
                logger.info("User {} does not exist locally, creating", username );

                String name  = userNode.get( "name" ).textValue();
                String email = userNode.get( "email" ).textValue();
                String dummyPassword = RandomStringUtils.randomAlphanumeric( 40 );

                JsonNode orgsNode = userNode.get( "organizations" );
                Iterator<String> fieldNames = orgsNode.fieldNames();

                if ( !fieldNames.hasNext() ) {
                    // no organizations for user exist in response from central Usergrid SSO
                    // so create user's personal organization and use username as organization name
                    fieldNames = Collections.singletonList( username ).iterator();
                }

                // create user and any organizations that user is supposed to have

                while ( fieldNames.hasNext() ) {

                    String orgName = fieldNames.next();

                    if ( userId == null ) {

                        // haven't created user yet so do that now
                        OrganizationOwnerInfo ownerOrgInfo = management.createOwnerAndOrganization(
                                orgName, username, name, email, dummyPassword, true, false );

                        applicationCreator.createSampleFor( ownerOrgInfo.getOrganization() );

                        userId = ownerOrgInfo.getOwner().getUuid();
                        userInfo = ownerOrgInfo.getOwner();

                        Counter createdAdminsCounter = getMetricsFactory().getCounter(
                            ManagementResource.class, SSO_CREATED_LOCAL_ADMINS );
                        createdAdminsCounter.inc();

                        logger.info( "Created user {} and org {}", username, orgName );

                    } else {

                        // already created user, so just create an org
                        final OrganizationInfo organization =
                            management.createOrganization( orgName, userInfo, true );

                        applicationCreator.createSampleFor( organization );

                        logger.info( "Created user {}'s other org {}", username, orgName );
                    }
                }

            }

            // store the external access_token as if it were one of our own
            management.importTokenForAdminUser( userId, extAccessToken, ttl );

            // success! return JSON object with access_token field
            accessInfo = new AccessInfo()
                    .withExpiresIn( tokens.getMaxTokenAgeInSeconds( extAccessToken ) )
                    .withAccessToken( extAccessToken );

        } catch (Exception e) {
            timerContext.stop();
            logger.debug("Error validating external token", e);
            throw e;
        }

        final Response response = Response.status( SC_OK )
            .type( jsonMediaType( callback ) ).entity( accessInfo ).build();

        timerContext.stop();

        return response;
    }

    /**
     * Look up Admin User via UG Central's /management/me endpoint.
     *
     * @param extAccessToken Access token issued by UG Central of Admin User
     * @return JsonNode representation of AccessInfo object for Admin User
     * @throws EntityNotFoundException if access_token is not valid.
     */
    private JsonNode getMeFromUgCentral( String extAccessToken )  throws EntityNotFoundException {

        // prepare to count tokens validated and rejected

        Counter tokensRejectedCounter = getMetricsFactory().getCounter(
            ManagementResource.class, SSO_TOKENS_REJECTED );
        Counter tokensValidatedCounter = getMetricsFactory().getCounter(
            ManagementResource.class, SSO_TOKENS_VALIDATED );

        // create URL of central Usergrid's /management/me endpoint

        String externalUrl = properties.getProperty( USERGRID_CENTRAL_URL ).trim();

        // be lenient about trailing slash
        externalUrl = !externalUrl.endsWith( "/" ) ? externalUrl + "/" : externalUrl;
        String me = externalUrl + "management/me?access_token=" + extAccessToken;

        // use our favorite HTTP client to GET /management/me

        Client client = getJerseyClient();
        final JsonNode accessInfoNode;
        try {
            accessInfoNode = client.target( me ).request()
                    .accept( MediaType.APPLICATION_JSON_TYPE )
                    .get(JsonNode.class);

            tokensValidatedCounter.inc();

        } catch ( Exception e ) {
            // user not found 404
            tokensRejectedCounter.inc();
            String msg = "Cannot find Admin User associated with " + extAccessToken;
            throw new EntityNotFoundException( msg, e );
        }

        return accessInfoNode;
    }


    private Client getJerseyClient() {

        if ( jerseyClient == null ) {

            synchronized ( this ) {

                // create HTTPClient and with configured connection pool

                int poolSize = 100; // connections
                final String poolSizeStr = properties.getProperty( CENTRAL_CONNECTION_POOL_SIZE );
                if ( poolSizeStr != null ) {
                    poolSize = Integer.parseInt( poolSizeStr );
                }

                PoolingClientConnectionManager connectionManager = new PoolingClientConnectionManager();
                connectionManager.setMaxTotal(poolSize);

                int timeout = 20000; // ms
                final String timeoutStr = properties.getProperty( CENTRAL_CONNECTION_TIMEOUT );
                if ( timeoutStr != null ) {
                    timeout = Integer.parseInt( timeoutStr );
                }

                int readTimeout = 20000; // ms
                final String readTimeoutStr = properties.getProperty( CENTRAL_READ_TIMEOUT );
                if ( readTimeoutStr != null ) {
                    readTimeout = Integer.parseInt( readTimeoutStr );
                }

                ClientConfig clientConfig = new ClientConfig();
                clientConfig.register( new JacksonFeature() );
                clientConfig.property( ApacheClientProperties.CONNECTION_MANAGER, connectionManager );
                clientConfig.connectorProvider( new ApacheConnectorProvider() );

                jerseyClient = ClientBuilder.newClient( clientConfig );
                jerseyClient.property( ClientProperties.CONNECT_TIMEOUT, timeout );
                jerseyClient.property( ClientProperties.READ_TIMEOUT, readTimeout );
            }
        }

        return jerseyClient;
    }


    /**
     * Check that authentication is allowed. If external token validation is enabled (Central Usergrid SSO)
     * then only superusers should be allowed to login directly to this Usergrid instance.
     */
    private void ensureAuthenticationAllowed( String username, String grant_type ) {

        if ( username == null || grant_type == null || !grant_type.equalsIgnoreCase( "password" )) {
            return; // we only care about username/password auth
        }

        final boolean externalTokensEnabled =
                !StringUtils.isEmpty( properties.getProperty( USERGRID_CENTRAL_URL ) );

        if ( externalTokensEnabled ) {

            // when external tokens enabled then only superuser can obtain an access token

            final String superuserName = properties.getProperty( USERGRID_SYSADMIN_LOGIN_NAME );
            if ( !username.equalsIgnoreCase( superuserName )) {

                // this guy is not the superuser
                throw new IllegalArgumentException( "Admin Users must login via " +
                        properties.getProperty( USERGRID_CENTRAL_URL ) );
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
