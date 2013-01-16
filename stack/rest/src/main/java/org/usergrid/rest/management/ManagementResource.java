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
package org.usergrid.rest.management;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.usergrid.utils.JsonUtils.mapToJsonString;
import static org.usergrid.utils.StringUtils.stringOrSubstringAfterFirst;
import static org.usergrid.utils.StringUtils.stringOrSubstringBeforeFirst;

import java.net.URLEncoder;
import java.util.Map;

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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.amber.oauth2.common.error.OAuthError;
import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.common.message.OAuthResponse;
import org.apache.amber.oauth2.common.message.types.GrantType;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.codec.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.usergrid.management.UserInfo;
import org.usergrid.management.exceptions.DisabledAdminUserException;
import org.usergrid.management.exceptions.UnactivatedAdminUserException;
import org.usergrid.rest.AbstractContextResource;
import org.usergrid.rest.exceptions.RedirectionException;
import org.usergrid.rest.management.organizations.OrganizationsResource;
import org.usergrid.rest.management.users.UsersResource;
import org.usergrid.security.oauth.AccessInfo;

import com.sun.jersey.api.view.Viewable;

@Path("/management")
@Component
@Scope("singleton")
@Produces({ MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
        "application/ecmascript", "text/jscript" })
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

    private static final Logger logger = LoggerFactory.getLogger(ManagementResource.class);

    public ManagementResource() {
        logger.info("ManagementResource initialized");
    }

    private static String wrapWithCallback(AccessInfo accessInfo, String callback) {
        return wrapWithCallback(mapToJsonString(accessInfo), callback);
    }

    private static String wrapWithCallback(String json, String callback) {
        if (StringUtils.isNotBlank(callback)) {
            json = callback + "(" + json + ")";
        }
        return json;
    }

    private static MediaType jsonMediaType(String callback) {
        return isNotBlank(callback) ? new MediaType("application", "javascript") : APPLICATION_JSON_TYPE;
    }

    @Path("organizations")
    public OrganizationsResource getOrganizations() {
        return getSubResource(OrganizationsResource.class);
    }

    @Path("orgs")
    public OrganizationsResource getOrganizations2() {
        return getSubResource(OrganizationsResource.class);
    }

    @Path("users")
    public UsersResource getUsers() {
        return getSubResource(UsersResource.class);
    }

    @GET
    @Path("token")
    public Response getAccessToken(@Context UriInfo ui, @HeaderParam("Authorization") String authorization,
            @QueryParam("grant_type") String grant_type, @QueryParam("username") String username,
            @QueryParam("password") String password, @QueryParam("client_id") String client_id,
            @QueryParam("client_secret") String client_secret, @QueryParam("ttl") long ttl,
            @QueryParam("callback") @DefaultValue("") String callback) throws Exception {

        logger.info("ManagementResource.getAccessToken with username: {}", username);

        UserInfo user = null;

        try {

            if (authorization != null) {
                String type = stringOrSubstringBeforeFirst(authorization, ' ').toUpperCase();
                if ("BASIC".equals(type)) {
                    String token = stringOrSubstringAfterFirst(authorization, ' ');
                    String[] values = Base64.decodeToString(token).split(":");
                    if (values.length >= 2) {
                        client_id = values[0].toLowerCase();
                        client_secret = values[1];
                    }
                }
            }

            String errorDescription = "invalid username or password";
            // do checking for different grant types
            if (GrantType.PASSWORD.toString().equals(grant_type)) {
                try {
                    user = management.verifyAdminUserPasswordCredentials(username, password);
                    if (user != null) {
                        logger.info("found user from verify: {}", user.getUuid());
                    }
                } catch (UnactivatedAdminUserException uaue) {
                    errorDescription = "user not activated";
                    logger.error("failed token check", uaue);
                } catch (DisabledAdminUserException daue) {
                    errorDescription = "user disabled";
                    logger.error("failed token check", daue);
                } catch (Exception e1) {
                    logger.error("failed token check", e1);
                }
            } else if ("client_credentials".equals(grant_type)) {
                try {
                    AccessInfo access_info = management.authorizeClient(client_id, client_secret, ttl);
                    if (access_info != null) {
                        return Response.status(SC_OK).type(jsonMediaType(callback))
                                .entity(wrapWithCallback(access_info, callback)).build();
                    }
                } catch (Exception e1) {
                    logger.error("failed authorizeClient", e1);
                }
            }

            if (user == null) {
                OAuthResponse response = OAuthResponse.errorResponse(SC_BAD_REQUEST)
                        .setError(OAuthError.TokenResponse.INVALID_GRANT)
                        .setErrorDescription(errorDescription).buildJSONMessage();
                return Response.status(response.getResponseStatus()).type(jsonMediaType(callback))
                        .entity(wrapWithCallback(response.getBody(), callback)).build();
            }

            String token = management.getAccessTokenForAdminUser(user.getUuid(), ttl);

            AccessInfo access_info = new AccessInfo().withExpiresIn(tokens.getMaxTokenAge(token) / 1000)
                    .withAccessToken(token)
                    .withProperty("user", management.getAdminUserOrganizationData(user.getUuid()));
            // increment counters for admin login
            management.countAdminUserAction(user, "login");

            return Response.status(SC_OK).type(jsonMediaType(callback)).entity(wrapWithCallback(access_info, callback))
                    .build();

        } catch (OAuthProblemException e) {
            logger.error("OAuth Error", e);
            OAuthResponse res = OAuthResponse.errorResponse(SC_BAD_REQUEST).error(e).buildJSONMessage();
            return Response.status(res.getResponseStatus()).type(jsonMediaType(callback))
                    .entity(wrapWithCallback(res.getBody(), callback)).build();
        }
    }

    @POST
    @Path("token")
    @Consumes(APPLICATION_FORM_URLENCODED)
    public Response getAccessTokenPost(@Context UriInfo ui, @FormParam("grant_type") String grant_type,
            @FormParam("username") String username, @FormParam("password") String password,
            @FormParam("client_id") String client_id, @FormParam("ttl") long ttl,
            @FormParam("client_secret") String client_secret, @QueryParam("callback") @DefaultValue("") String callback)
            throws Exception {

        logger.info("ManagementResource.getAccessTokenPost");

        return getAccessToken(ui, null, grant_type, username, password, client_id, client_secret, ttl, callback);
    }

    @POST
    @Path("token")
    @Consumes(APPLICATION_JSON)
    public Response getAccessTokenPostJson(@Context UriInfo ui, Map<String, Object> json,
            @QueryParam("callback") @DefaultValue("") String callback) throws Exception {

        String grant_type = (String) json.get("grant_type");
        String username = (String) json.get("username");
        String password = (String) json.get("password");
        String client_id = (String) json.get("client_id");
        String client_secret = (String) json.get("client_secret");
        long ttl = 0;

        if (json.get("ttl") != null) {
            try {
                ttl = Long.parseLong(json.get("ttl").toString());
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("ttl must be a number >= 0");
            }
        }

        return getAccessToken(ui, null, grant_type, username, password, client_id, client_secret, ttl, callback);
    }

    @GET
    @Path("authorize")
    @Produces(MediaType.TEXT_HTML)
    public Viewable showAuthorizeForm(@Context UriInfo ui, @QueryParam("response_type") String response_type,
            @QueryParam("client_id") String client_id, @QueryParam("redirect_uri") String redirect_uri,
            @QueryParam("scope") String scope, @QueryParam("state") String state) {

        responseType = response_type;
        clientId = client_id;
        redirectUri = redirect_uri;
        this.scope = scope;
        this.state = state;

        return handleViewable("authorize_form", this);
    }

    @POST
    @Path("authorize")
    @Produces(MediaType.TEXT_HTML)
    public Viewable handleAuthorizeForm(@Context UriInfo ui, @FormParam("response_type") String response_type,
            @FormParam("client_id") String client_id, @FormParam("redirect_uri") String redirect_uri,
            @FormParam("scope") String scope, @FormParam("state") String state, @FormParam("username") String username,
            @FormParam("password") String password) {

        try {
            responseType = response_type;
            clientId = client_id;
            redirectUri = redirect_uri;
            this.scope = scope;
            this.state = state;

            UserInfo user = null;
            try {
                user = management.verifyAdminUserPasswordCredentials(username, password);
            } catch (Exception e1) {
            }
            if ((user != null) && isNotBlank(redirect_uri)) {
                if (!redirect_uri.contains("?")) {
                    redirect_uri += "?";
                } else {
                    redirect_uri += "&";
                }
                redirect_uri += "code=" + management.getAccessTokenForAdminUser(user.getUuid(), 0);
                if (isNotBlank(state)) {
                    redirect_uri += "&state=" + URLEncoder.encode(state, "UTF-8");
                }
                throw new RedirectionException(state);
            } else {
                errorMsg = "Username or password do not match";
            }

            return handleViewable("authorize_form", this);
        } catch (RedirectionException e) {
            throw e;
        } catch (Exception e) {
            return handleViewable("error", e);
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
