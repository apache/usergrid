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
package org.usergrid.rest.management;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.temporaryRedirect;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.usergrid.utils.JsonUtils.mapToJsonString;
import static org.usergrid.utils.StringUtils.stringOrSubstringAfterFirst;
import static org.usergrid.utils.StringUtils.stringOrSubstringBeforeFirst;

import java.net.URI;
import java.net.URLEncoder;

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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.amber.oauth2.common.error.OAuthError;
import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.common.message.OAuthResponse;
import org.apache.amber.oauth2.common.message.types.GrantType;
import org.apache.shiro.codec.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.usergrid.management.UserInfo;
import org.usergrid.rest.AbstractContextResource;
import org.usergrid.security.oauth.AccessInfo;

import com.sun.jersey.api.json.JSONWithPadding;
import com.sun.jersey.api.view.Viewable;

@Path("/management")
@Component
@Scope("singleton")
@Produces({ MediaType.APPLICATION_JSON, "application/javascript",
		"application/x-javascript", "text/ecmascript",
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

	private static final Logger logger = LoggerFactory
			.getLogger(ManagementResource.class);

	public ManagementResource() {
		logger.info("ManagementResource initialized");
	}

	@GET
	@Path("token")
	public JSONWithPadding getAccessToken(@Context UriInfo ui,
			@HeaderParam("Authorization") String authorization,
			@QueryParam("grant_type") String grant_type,
			@QueryParam("username") String username,
			@QueryParam("password") String password,
			@QueryParam("client_id") String client_id,
			@QueryParam("client_secret") String client_secret,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		logger.info("ManagementResource.getAccessToken");

		UserInfo user = null;

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
					user = management.verifyAdminUserPasswordCredentials(
							username, password);
				} catch (Exception e1) {
				}
			} else if ("client_credentials".equals(grant_type)) {
				try {
					AccessInfo access_info = management.authorizeClient(
							client_id, client_secret);
					if (access_info != null) {
						return new JSONWithPadding(Response.status(SC_OK)
								.type(APPLICATION_JSON_TYPE)
								.entity(mapToJsonString(access_info)).build(),
								callback);
					}
				} catch (Exception e1) {
				}
			}

			if (user == null) {
				OAuthResponse response = OAuthResponse
						.errorResponse(SC_BAD_REQUEST)
						.setError(OAuthError.TokenResponse.INVALID_GRANT)
						.setErrorDescription("invalid username or password")
						.buildJSONMessage();
				return new JSONWithPadding(Response
						.status(response.getResponseStatus())
						.type(APPLICATION_JSON_TYPE).entity(response.getBody())
						.build(), callback);
			}

			AccessInfo access_info = new AccessInfo()
					.withExpiresIn(3600)
					.withAccessToken(
							management.getAccessTokenForAdminUser(user
									.getUuid()))
					.withProperty(
							"user",
							management.getAdminUserOrganizationData(user
									.getUuid()));

			return new JSONWithPadding(Response.status(SC_OK)
					.type(APPLICATION_JSON_TYPE)
					.entity(mapToJsonString(access_info)).build(), callback);

		} catch (OAuthProblemException e) {
			logger.error("OAuth Error", e);
			OAuthResponse res = OAuthResponse.errorResponse(SC_BAD_REQUEST)
					.error(e).buildJSONMessage();
			return new JSONWithPadding(Response.status(res.getResponseStatus())
					.type(APPLICATION_JSON_TYPE).entity(res.getBody()).build(),
					callback);
		}
	}

	@POST
	@Path("token")
	@Consumes(APPLICATION_FORM_URLENCODED)
	public JSONWithPadding getAccessTokenPost(@Context UriInfo ui,
			@FormParam("grant_type") String grant_type,
			@FormParam("username") String username,
			@FormParam("password") String password,
			@FormParam("client_id") String client_id,
			@FormParam("client_secret") String client_secret,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		logger.info("ManagementResource.getAccessTokenPost");

		return getAccessToken(ui, null, grant_type, username, password,
				client_id, client_secret, callback);
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

		UserInfo user = null;
		try {
			user = management.verifyAdminUserPasswordCredentials(username,
					password);
		} catch (Exception e1) {
		}
		if ((user != null) && isNotBlank(redirect_uri)) {
			if (!redirect_uri.contains("?")) {
				redirect_uri += "?";
			} else {
				redirect_uri += "&";
			}
			redirect_uri += "code="
					+ management.getAccessTokenForAdminUser(user.getUuid());
			if (isNotBlank(state)) {
				redirect_uri += "&state=" + URLEncoder.encode(state, "UTF-8");
			}
			throw new WebApplicationException(temporaryRedirect(new URI(state))
					.build());
		} else {
			errorMsg = "Username or password do not match";
		}

		return new Viewable("authorize_form", this);
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
