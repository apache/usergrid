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
import static org.usergrid.rest.utils.JSONPUtils.jsonMediaType;
import static org.usergrid.rest.utils.JSONPUtils.wrapJSONPResponse;
import static org.usergrid.rest.utils.JSONPUtils.wrapWithCallback;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import org.usergrid.persistence.entities.User;
import org.usergrid.rest.AbstractContextResource;
import org.usergrid.security.oauth.AccessInfo;
import org.usergrid.services.ServiceManager;

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

	@POST
	@Path("facebook")
	@Consumes(APPLICATION_FORM_URLENCODED)
	public Response authFBPost(@Context UriInfo ui,
			@FormParam("fb_access_token") String fb_access_token,
			@QueryParam("ttl") long ttl,
			@QueryParam("callback") @DefaultValue("") String callback)
			throws Exception {

		logger.info("AuthResource.authFBPost");

		return authFB(ui, fb_access_token, ttl, callback);
	}

	@GET
	@Path("facebook")
	public Response authFB(@Context UriInfo ui,
			@QueryParam("fb_access_token") String fb_access_token,
			@QueryParam("ttl") long ttl,
			@QueryParam("callback") @DefaultValue("") String callback)
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

			User user = management.getOrCreateUserForFacebookAccessToken(
					services.getEntityManager().getApplicationRef().getUuid(),
					fb_access_token);

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

			String token = management.getAccessTokenForAppUser(
					services.getApplicationId(), user.getUuid(), ttl);

			AccessInfo access_info = new AccessInfo()
					.withExpiresIn(tokens.getMaxTokenAge(token) / 1000)
					.withAccessToken(token).withProperty("user", user);

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

	@POST
	@Path("foursquare")
	@Consumes(APPLICATION_FORM_URLENCODED)
	public Response authFQPost(@Context UriInfo ui,
			@FormParam("fq_access_token") String fq_access_token,
			@QueryParam("ttl") long ttl,
			@QueryParam("callback") @DefaultValue("") String callback)
			throws Exception {

		logger.info("AuthResource.authFQPost");

		return authFQ(ui, fq_access_token, ttl, callback);
	}

	@GET
	@Path("foursquare")
	public Response authFQ(@Context UriInfo ui,
			@QueryParam("fq_access_token") String fq_access_token,
			@QueryParam("ttl") long ttl,
			@QueryParam("callback") @DefaultValue("") String callback)
			throws Exception {

		logger.info("AuthResource.authFQ");

		try {
			if (StringUtils.isEmpty(fq_access_token)) {
				logger.error("Missing FQ Access token");
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

			User user = management.getOrCreateUserForFoursquareAccessToken(
					services.getEntityManager().getApplicationRef().getUuid(),
					fq_access_token);

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

			String token = management.getAccessTokenForAppUser(
					services.getApplicationId(), user.getUuid(), ttl);

			AccessInfo access_info = new AccessInfo()
					.withExpiresIn(tokens.getMaxTokenAge(token) / 1000)
					.withAccessToken(token).withProperty("user", user);

			return Response.status(SC_OK).type(jsonMediaType(callback))
					.entity(wrapWithCallback(access_info, callback)).build();
		} catch (Exception e) {
			logger.error("FQ Auth Error", e);
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
