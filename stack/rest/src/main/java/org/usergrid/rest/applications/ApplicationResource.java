/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Stack.
 * 
 * Usergrid Stack is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * 
 * Usergrid Stack is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License along
 * with Usergrid Stack. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU AGPL version 3 section 7
 * 
 * Linking Usergrid Stack statically or dynamically with other modules is making
 * a combined work based on Usergrid Stack. Thus, the terms and conditions of the
 * GNU General Public License cover the whole combination.
 * 
 * In addition, as a special exception, the copyright holders of Usergrid Stack
 * give you permission to combine Usergrid Stack with free software programs or
 * libraries that are released under the GNU LGPL and with independent modules
 * that communicate with Usergrid Stack solely through:
 * 
 *   - Classes implementing the org.usergrid.services.Service interface
 *   - Apache Shiro Realms and Filters
 *   - Servlet Filters and JAX-RS/Jersey Filters
 * 
 * You may copy and distribute such a system following the terms of the GNU AGPL
 * for Usergrid Stack and the licenses of the other code concerned, provided that
 ******************************************************************************/
package org.usergrid.rest.applications;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.temporaryRedirect;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.usergrid.security.shiro.utils.SubjectUtils.isApplicationAdmin;
import static org.usergrid.services.ServiceParameter.addParameter;
import static org.usergrid.utils.JsonUtils.mapToJsonString;
import static org.usergrid.utils.StringUtils.stringOrSubstringAfterFirst;
import static org.usergrid.utils.StringUtils.stringOrSubstringBeforeFirst;

import java.net.URI;
import java.net.URLEncoder;
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
import javax.ws.rs.WebApplicationException;
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
import org.usergrid.management.ApplicationInfo;
import org.usergrid.mq.QueueManager;
import org.usergrid.persistence.Identifier;
import org.usergrid.persistence.entities.User;
import org.usergrid.rest.ApiResponse;
import org.usergrid.rest.applications.queues.QueueResource;
import org.usergrid.rest.applications.users.UsersResource;
import org.usergrid.rest.security.annotations.RequireApplicationAccess;
import org.usergrid.security.oauth.AccessInfo;
import org.usergrid.security.oauth.ClientCredentialsInfo;

import com.sun.jersey.api.json.JSONWithPadding;
import com.sun.jersey.api.view.Viewable;

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

	@Path("users")
	public UsersResource getUsers(@Context UriInfo ui) throws Exception {

		logger.info("ServiceResource.addNameParameter");

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
							client_id, client_secret);
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

			AccessInfo access_info = new AccessInfo()
					.withExpiresIn(3600)
					.withAccessToken(
							management.getAccessTokenForAppUser(
									services.getApplicationId(), user.getUuid()))
					.withProperty("user", user);

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
			@FormParam("redirect_uri") String redirect_uri,
			@QueryParam("callback") @DefaultValue("") String callback)
			throws Exception {

		logger.info("ApplicationResource.getAccessTokenPost");

		return getAccessToken(ui, null, grant_type, username, password, pin,
				client_id, client_secret, code, redirect_uri, callback);
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
			@QueryParam("scope") String scope, @QueryParam("state") String state)
			throws Exception {

		responseType = response_type;
		clientId = client_id;
		redirectUri = redirect_uri;
		this.scope = scope;
		this.state = state;

		ApplicationInfo app = management.getApplication(applicationId);
		applicationName = app.getName();

		return new Viewable("authorize_form", this);
	}

	@POST
	@Path("authorize")
	public Viewable handleAuthorizeForm(@Context UriInfo ui,
			@FormParam("response_type") String response_type,
			@FormParam("client_id") String client_id,
			@FormParam("redirect_uri") String redirect_uri,
			@FormParam("scope") String scope, @FormParam("state") String state,
			@FormParam("username") String username,
			@FormParam("password") String password) throws Exception {

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
							services.getApplicationId(), user.getUuid());
			if (isNotBlank(state)) {
				redirect_uri += "&state=" + URLEncoder.encode(state, "UTF-8");
			}
			throw new WebApplicationException(temporaryRedirect(new URI(state))
					.build());
		} else {
			errorMsg = "Username or password do not match";
		}

		ApplicationInfo app = management.getApplication(applicationId);
		applicationName = app.getName();

		return new Viewable("authorize_form", this);
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
