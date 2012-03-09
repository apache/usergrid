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
import static org.usergrid.rest.utils.JSONPUtils.jsonMediaType;
import static org.usergrid.rest.utils.JSONPUtils.wrapJSONPResponse;
import static org.usergrid.rest.utils.JSONPUtils.wrapWithCallback;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.amber.oauth2.common.error.OAuthError;
import org.apache.amber.oauth2.common.message.OAuthResponse;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.entities.User;
import org.usergrid.rest.AbstractContextResource;
import org.usergrid.security.oauth.AccessInfo;
import org.usergrid.services.ServiceManager;
import org.usergrid.utils.JsonUtils;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;

@Component
@Scope("prototype")
@Produces({ MediaType.APPLICATION_JSON, "application/javascript",
		"application/x-javascript", "text/ecmascript",
		"application/ecmascript", "text/jscript" })
public class AuthResource extends AbstractContextResource {

	private static final Logger logger = LoggerFactory
			.getLogger(AuthResource.class);

	ServiceManager services = null;

	public AuthResource() {
	}

	@Override
	public void setParent(AbstractContextResource parent) {
		super.setParent(parent);
		if (parent instanceof ServiceResource) {
			services = ((ServiceResource) parent).services;
		}
	}

	@GET
	@Path("fb")
	public Response authFB(@Context UriInfo ui,
			@QueryParam("fb_access_token") String fb_access_token,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		logger.info("AuthResource.authFB");

		try {
			if (StringUtils.isEmpty(fb_access_token)) {
				logger.error("Missing FB Access token");
				OAuthResponse response = OAuthResponse
						.errorResponse(SC_BAD_REQUEST)
						.setError(OAuthError.TokenResponse.INVALID_REQUEST)
						.setErrorDescription("missing access token")
						.buildJSONMessage();
				return Response
						.status(response.getResponseStatus())
						.type(jsonMediaType(callback))
						.entity(wrapJSONPResponse(callback, response.getBody()))
						.build();
			}

			FacebookClient facebookClient = new DefaultFacebookClient(
					fb_access_token);

			com.restfb.types.User fb_user = facebookClient.fetchObject("me",
					com.restfb.types.User.class);

			User user = null;

			if (fb_user != null) {
				EntityManager em = services.getEntityManager();
				Results r = em.searchCollection(em.getApplicationRef(),
						"users",
						Query.findForProperty("facebook.id", fb_user.getId()));

				if (r.size() > 1) {
					logger.error("Multiple users for FB ID: " + fb_user.getId());
					OAuthResponse response = OAuthResponse
							.errorResponse(SC_BAD_REQUEST)
							.setError(OAuthError.TokenResponse.INVALID_REQUEST)
							.setErrorDescription(
									"multiple users with same Facebook ID")
							.buildJSONMessage();
					return Response
							.status(response.getResponseStatus())
							.type(jsonMediaType(callback))
							.entity(wrapJSONPResponse(callback,
									response.getBody())).build();
				}

				if (r.size() < 1) {
					Map<String, Object> fb_map = JsonUtils.toJsonMap(fb_user);
					Map<String, Object> properties = new LinkedHashMap<String, Object>();
					properties.put("facebook", fb_map);
					properties.put(
							"username",
							fb_user.getUsername() != null ? fb_user
									.getUsername() : "fb_" + fb_user.getId());
					properties.put("name", fb_user.getName());
					if (fb_user.getEmail() != null) {
						properties.put("email", fb_user.getEmail());
					}
					user = em.create("user", User.class, properties);
				} else {
					user = (User) r.getEntity().toTypedEntity();
					Map<String, Object> fb_map = JsonUtils.toJsonMap(fb_user);
					Map<String, Object> properties = new LinkedHashMap<String, Object>();
					properties.put("facebook", fb_map);
					em.setProperty(user, "facebook", fb_map);
					user.setProperty("facebook", fb_map);
				}
			}

			if (user == null) {
				logger.error("Unable to find or create user");
				OAuthResponse response = OAuthResponse
						.errorResponse(SC_BAD_REQUEST)
						.setError(OAuthError.TokenResponse.INVALID_REQUEST)
						.setErrorDescription("invalid user").buildJSONMessage();
				return Response
						.status(response.getResponseStatus())
						.type(jsonMediaType(callback))
						.entity(wrapJSONPResponse(callback, response.getBody()))
						.build();
			}

			AccessInfo access_info = new AccessInfo()
					.withExpiresIn(management.getMaxTokenAge() / 1000)
					.withAccessToken(
							management.getAccessTokenForAppUser(
									services.getApplicationId(), user.getUuid()))
					.withProperty("user", user);

			return Response.status(SC_OK).type(jsonMediaType(callback))
					.entity(wrapWithCallback(access_info, callback)).build();
		} catch (Exception e) {
			logger.error("FB Auth Error", e);
			OAuthResponse response = OAuthResponse
					.errorResponse(SC_BAD_REQUEST)
					.setError(OAuthError.TokenResponse.INVALID_REQUEST)
					.buildJSONMessage();
			return Response.status(response.getResponseStatus())
					.type(jsonMediaType(callback))
					.entity(wrapJSONPResponse(callback, response.getBody()))
					.build();
		}
	}

}
