/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.rest.applications;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.usergrid.security.shiro.utils.SubjectUtils.isApplicationAdmin;
import static org.usergrid.services.ServiceParameter.addParameter;
import static org.usergrid.utils.JsonUtils.mapToJsonString;
import static org.usergrid.utils.StringUtils.stringOrSubstringAfterFirst;
import static org.usergrid.utils.StringUtils.stringOrSubstringBeforeFirst;

import java.net.URLEncoder;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.amber.oauth2.common.error.OAuthError;
import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.common.message.OAuthResponse;
import org.apache.amber.oauth2.common.message.types.GrantType;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.codec.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.mq.QueueManager;
import org.usergrid.persistence.Identifier;
import org.usergrid.persistence.entities.User;
import org.usergrid.rest.ApiResponse;
import org.usergrid.rest.applications.events.EventsResource;
import org.usergrid.rest.applications.queues.QueueResource;
import org.usergrid.rest.applications.users.UsersResource;
import org.usergrid.rest.exceptions.RedirectionException;
import org.usergrid.rest.security.annotations.RequireApplicationAccess;
import org.usergrid.security.oauth.AccessInfo;
import org.usergrid.security.oauth.ClientCredentialsInfo;

import com.sun.jersey.api.json.JSONWithPadding;
import com.sun.jersey.api.view.Viewable;

@Component("org.usergrid.rest.applications.ApplicationResource")
@Scope("prototype")
@Produces({ MediaType.APPLICATION_JSON, "application/javascript",
        "application/x-javascript", "text/ecmascript",
        "application/ecmascript", "text/jscript" })
public class ApplicationResource extends ServiceResource {

    public static final Logger logger = LoggerFactory
            .getLogger(ServiceResource.class);

    UUID applicationId;
    QueueManager queues;

    public ApplicationResource() {
    }

    public ApplicationResource init(UUID applicationId) throws Exception {
        this.applicationId = applicationId;
        services = smf.getServiceManager(applicationId);
        queues = qmf.getQueueManager(applicationId);
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
        return getSubResource(AuthResource.class);
    }

    @RequireApplicationAccess
    @Path("queues")
    public QueueResource getQueueResource() throws Exception {
        return getSubResource(QueueResource.class).init(queues, "");
    }

    @RequireApplicationAccess
    @Path("events")
    public EventsResource getEventsResource(@Context UriInfo ui)
            throws Exception {
        addParameter(getServiceParameters(), "events");

        PathSegment ps = getFirstPathSegment("events");
        if (ps != null) {
            addMatrixParams(getServiceParameters(), ui, ps);
        }

        return getSubResource(EventsResource.class);
    }

    @RequireApplicationAccess
    @Path("event")
    public EventsResource getEventResource(@Context UriInfo ui)
            throws Exception {
        return getEventsResource(ui);
    }

    @Path("users")
    public UsersResource getUsers(@Context UriInfo ui) throws Exception {
        logger.info("ApplicationResource.getUsers");
        addParameter(getServiceParameters(), "users");

        PathSegment ps = getFirstPathSegment("users");
        if (ps != null) {
            addMatrixParams(getServiceParameters(), ui, ps);
        }

        return getSubResource(UsersResource.class);
    }

    @Path("user")
    public UsersResource getUsers2(@Context UriInfo ui) throws Exception {
        return getUsers(ui);
    }

    private static String wrapWithCallback(AccessInfo accessInfo,
            String callback) {
        return wrapWithCallback(mapToJsonString(accessInfo), callback);
    }

    private static String wrapWithCallback(String json, String callback) {
        if (StringUtils.isNotBlank(callback)) {
            json = callback + "(" + json + ")";
        }
        return json;
    }

    private static MediaType jsonMediaType(String callback) {
        return isNotBlank(callback) ? new MediaType("application", "javascript")
                : APPLICATION_JSON_TYPE;
    }

    @GET
    @Path("token")
    public Response getAccessToken(@Context UriInfo ui,
            @HeaderParam("Authorization") String authorization,
            @QueryParam("grant_type") String grant_type,
            @QueryParam("username") String username,
            @QueryParam("password") String password,
            @QueryParam("pin") String pin,
            @QueryParam("client_id") String client_id,
            @QueryParam("client_secret") String client_secret,
            @QueryParam("code") String code,
            @QueryParam("ttl") long ttl,
            @QueryParam("redirect_uri") String redirect_uri,
            @QueryParam("callback") @DefaultValue("") String callback)
            throws Exception {

        logger.info("ApplicationResource.getAccessToken");

        User user = null;

        try {

            if (authorization != null) {
                String type = stringOrSubstringBeforeFirst(authorization, ' ')
                        .toUpperCase();
                if ("BASIC".equals(type)) {
                    String token = stringOrSubstringAfterFirst(authorization,
                            ' ');
                    String[] values = Base64.decodeToString(token).split(":");
                    if (values.length >= 2) {
                        client_id = values[0].toLowerCase();
                        client_secret = values[1];
                    }
                }
            }

            // do checking for different grant types
            if (GrantType.PASSWORD.toString().equals(grant_type)) {
                try {
                    user = management.verifyAppUserPasswordCredentials(
                            services.getApplicationId(), username, password);
                } catch (Exception e1) {
                }
            } else if ("pin".equals(grant_type)) {
                try {
                    user = management.verifyAppUserPinCredentials(
                            services.getApplicationId(), username, pin);
                } catch (Exception e1) {
                }
            } else if ("client_credentials".equals(grant_type)) {
                try {
                    AccessInfo access_info = management.authorizeClient(
                            client_id, client_secret, ttl);
                    if (access_info != null) {
                        return Response
                                .status(SC_OK)
                                .type(jsonMediaType(callback))
                                .entity(wrapWithCallback(access_info, callback))
                                .build();
                    }
                } catch (Exception e1) {
                }
            } else if ("authorization_code".equals(grant_type)) {
                AccessInfo access_info = new AccessInfo();
                access_info.setAccessToken(code);
                return Response.status(SC_OK).type(jsonMediaType(callback))
                        .entity(wrapWithCallback(access_info, callback))
                        .build();
            }

            if (user == null) {
                OAuthResponse response = OAuthResponse
                        .errorResponse(SC_BAD_REQUEST)
                        .setError(OAuthError.TokenResponse.INVALID_GRANT)
                        .setErrorDescription("invalid username or password")
                        .buildJSONMessage();
                return Response.status(response.getResponseStatus())
                        .type(jsonMediaType(callback))
                        .entity(wrapWithCallback(response.getBody(), callback))
                        .build();
            }

            String token = management.getAccessTokenForAppUser(
                    services.getApplicationId(), user.getUuid(), ttl);

            AccessInfo access_info = new AccessInfo()
                    .withExpiresIn(tokens.getMaxTokenAge(token) / 1000)
                    .withAccessToken(token).withProperty("user", user);

            return Response.status(SC_OK).type(jsonMediaType(callback))
                    .entity(wrapWithCallback(access_info, callback)).build();

        } catch (OAuthProblemException e) {
            logger.error("OAuth Error", e);
            OAuthResponse res = OAuthResponse.errorResponse(SC_BAD_REQUEST)
                    .error(e).buildJSONMessage();
            return Response.status(res.getResponseStatus())
                    .type(jsonMediaType(callback))
                    .entity(wrapWithCallback(res.getBody(), callback)).build();
        }
    }

    @POST
    @Path("token")
    @Consumes(APPLICATION_FORM_URLENCODED)
    public Response getAccessTokenPost(@Context UriInfo ui,
            @FormParam("grant_type") String grant_type,
            @FormParam("username") String username,
            @FormParam("password") String password,
            @FormParam("pin") String pin,
            @FormParam("client_id") String client_id,
            @FormParam("client_secret") String client_secret,
            @FormParam("code") String code,
            @FormParam("ttl") long ttl,
            @FormParam("redirect_uri") String redirect_uri,
            @QueryParam("callback") @DefaultValue("") String callback)
            throws Exception {

        logger.info("ApplicationResource.getAccessTokenPost");

        return getAccessToken(ui, null, grant_type, username, password, pin,
                client_id, client_secret, code, ttl, redirect_uri, callback);
    }

    @POST
    @Path("token")
    @Consumes(APPLICATION_JSON)
    public Response getAccessTokenPostJson(@Context UriInfo ui,
            Map<String, Object> json,
            @QueryParam("callback") @DefaultValue("") String callback)
            throws Exception {

        String grant_type = (String) json.get("grant_type");
        String username = (String) json.get("username");
        String password = (String) json.get("password");
        String client_id = (String) json.get("client_id");
        String client_secret = (String) json.get("client_secret");
        String pin = (String) json.get("pin");
        String code = (String) json.get("code");
        String redirect_uri = (String) json.get("redirect_uri");
        long ttl = 0;

        if (json.get("ttl") != null) {
            try {
                ttl = Long.parseLong(json.get("ttl").toString());
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("ttl must be a number >= 0");
            }
        }
        
        return getAccessToken(ui, null, grant_type, username, password, pin,
                client_id, client_secret, code, ttl, redirect_uri, callback);
    }

    @GET
    @Path("credentials")
    @RequireApplicationAccess
    public JSONWithPadding getKeys(@Context UriInfo ui,
            @QueryParam("callback") @DefaultValue("callback") String callback)
            throws Exception {

        logger.info("AuthResource.keys");

        if (!isApplicationAdmin(Identifier.fromUUID(applicationId))) {
            throw new UnauthorizedException();
        }

        ClientCredentialsInfo kp = new ClientCredentialsInfo(
                management.getClientIdForApplication(services
                        .getApplicationId()),
                management.getClientSecretForApplication(services
                        .getApplicationId()));

        return new JSONWithPadding(new ApiResponse(ui).withCredentials(kp)
                .withAction("get application keys").withSuccess(), callback);
    }

    @POST
    @Path("credentials")
    @RequireApplicationAccess
    public JSONWithPadding generateKeys(@Context UriInfo ui,
            @QueryParam("callback") @DefaultValue("callback") String callback)
            throws Exception {

        logger.info("AuthResource.keys");

        if (!isApplicationAdmin(Identifier.fromUUID(applicationId))) {
            throw new UnauthorizedException();
        }

        ClientCredentialsInfo kp = new ClientCredentialsInfo(
                management.getClientIdForApplication(services
                        .getApplicationId()),
                management.newClientSecretForApplication(services
                        .getApplicationId()));

        return new JSONWithPadding(new ApiResponse(ui).withCredentials(kp)
                .withAction("generate application keys").withSuccess(),
                callback);
    }

    @GET
    @Path("authorize")
    public Viewable showAuthorizeForm(@Context UriInfo ui,
            @QueryParam("response_type") String response_type,
            @QueryParam("client_id") String client_id,
            @QueryParam("redirect_uri") String redirect_uri,
            @QueryParam("scope") String scope, @QueryParam("state") String state) {

        try {
            responseType = response_type;
            clientId = client_id;
            redirectUri = redirect_uri;
            this.scope = scope;
            this.state = state;

            ApplicationInfo app = management.getApplicationInfo(applicationId);
            applicationName = app.getName();

            return handleViewable("authorize_form", this);
        } catch (RedirectionException e) {
            throw e;
        } catch (Exception e) {
            return handleViewable("error", e);
        }
    }

    @POST
    @Path("authorize")
    public Viewable handleAuthorizeForm(@Context UriInfo ui,
            @FormParam("response_type") String response_type,
            @FormParam("client_id") String client_id,
            @FormParam("redirect_uri") String redirect_uri,
            @FormParam("scope") String scope, @FormParam("state") String state,
            @FormParam("username") String username,
            @FormParam("password") String password) {

        try {
            responseType = response_type;
            clientId = client_id;
            redirectUri = redirect_uri;
            this.scope = scope;
            this.state = state;

            User user = null;
            try {
                user = management.verifyAppUserPasswordCredentials(
                        services.getApplicationId(), username, password);
            } catch (Exception e1) {
            }
            if ((user != null) && isNotBlank(redirect_uri)) {
                if (!redirect_uri.contains("?")) {
                    redirect_uri += "?";
                } else {
                    redirect_uri += "&";
                }
                redirect_uri += "code="
                        + management.getAccessTokenForAppUser(
                                services.getApplicationId(), user.getUuid(), 0);
                if (isNotBlank(state)) {
                    redirect_uri += "&state="
                            + URLEncoder.encode(state, "UTF-8");
                }
                throw new RedirectionException(state);
            } else {
                errorMsg = "Username or password do not match";
            }

            ApplicationInfo app = management.getApplicationInfo(applicationId);
            applicationName = app.getName();

            return handleViewable("authorize_form", this);
        } catch (RedirectionException e) {
            throw e;
        } catch (Exception e) {
            return handleViewable("error", e);
        }
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

}
