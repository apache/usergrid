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
package org.usergrid.rest.applications.users;

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

import static org.usergrid.security.shiro.utils.SubjectUtils.isApplicationAdmin;
import static org.usergrid.utils.ConversionUtils.string;

import java.util.Map;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import net.tanesha.recaptcha.ReCaptchaImpl;
import net.tanesha.recaptcha.ReCaptchaResponse;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.Identifier;
import org.usergrid.persistence.entities.User;
import org.usergrid.rest.AbstractContextResource;
import org.usergrid.rest.ApiResponse;
import org.usergrid.rest.applications.ServiceResource;
import org.usergrid.rest.security.annotations.RequireApplicationAccess;
import org.usergrid.security.tokens.exceptions.TokenException;

import com.sun.jersey.api.json.JSONWithPadding;
import com.sun.jersey.api.view.Viewable;

@Component("org.usergrid.rest.applications.users.UserResource")
@Scope("prototype")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource extends ServiceResource {

	public static final String USER_EXTENSION_RESOURCE_PREFIX = "org.usergrid.rest.applications.users.extensions.";

	private static final Logger logger = LoggerFactory
			.getLogger(UserResource.class);

	User user;

	Identifier userIdentifier;

	String errorMsg;

	String token;

	public UserResource() {
	}

	public UserResource init(Identifier userIdentifier) throws Exception {
		this.userIdentifier = userIdentifier;
		return this;
	}

	@PUT
	@Path("password")
	public JSONWithPadding setUserPasswordPut(@Context UriInfo ui,
			Map<String, Object> json,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		logger.info("UserResource.setUserPassword");

		if (json == null) {
			return null;
		}

		ApiResponse response = new ApiResponse(ui);
		response.setAction("set user password");
		String oldPassword = string(json.get("oldpassword"));
		String newPassword = string(json.get("newpassword"));
		if (StringUtils.isBlank(oldPassword)
				|| StringUtils.isBlank(newPassword)) {
			throw new IllegalArgumentException(
					"oldpassword and newpassword are both required");
		}
		if (getUser() != null) {

			if (isApplicationAdmin(Identifier.fromUUID(getApplicationId()))) {
				management
						.setAdminUserPassword(getApplicationId(), newPassword);
			} else {
				management.setAppUserPassword(getApplicationId(), getUser()
						.getUuid(), oldPassword, newPassword);
			}
		} else {
			response.setError("User not found");
		}

		return new JSONWithPadding(response, callback);
	}

	@POST
	@Path("password")
	public JSONWithPadding setUserPasswordPost(@Context UriInfo ui,
			Map<String, Object> json,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {
		return setUserPasswordPut(ui, json, callback);
	}

	@GET
	@Path("sendpin")
	public JSONWithPadding sendPin(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		logger.info("UserResource.sendPin");

		ApiResponse response = new ApiResponse(ui);
		response.setAction("retrieve user pin");

		if (getUser() != null) {
			management.sendAppUserPin(getApplicationId(), getUserUuid());
		} else {
			response.setError("User not found");
		}

		return new JSONWithPadding(response, callback);
	}

	@POST
	@Path("sendpin")
	public JSONWithPadding postSendPin(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {
		return sendPin(ui, callback);
	}

	@GET
	@Path("setpin")
	@RequireApplicationAccess
	public JSONWithPadding setPin(@Context UriInfo ui,
			@QueryParam("pin") String pin,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		logger.info("UserResource.setPin");

		ApiResponse response = new ApiResponse(ui);
		response.setAction("set user pin");

		if (getUser() != null) {
			management.setAppUserPin(getApplicationId(), getUserUuid(), pin);
		} else {
			response.setError("User not found");
		}

		return new JSONWithPadding(response, callback);
	}

	@POST
	@Path("setpin")
	@Consumes("application/x-www-form-urlencoded")
	@RequireApplicationAccess
	public JSONWithPadding postPin(@Context UriInfo ui,
			@FormParam("pin") String pin,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		logger.info("UserResource.postPin");

		ApiResponse response = new ApiResponse(ui);
		response.setAction("set user pin");

		if (getUser() != null) {
			management.setAppUserPin(getApplicationId(), getUserUuid(), pin);
		} else {
			response.setError("User not found");
		}

		return new JSONWithPadding(response, callback);
	}

	@POST
	@Path("setpin")
	@Consumes(MediaType.APPLICATION_JSON)
	@RequireApplicationAccess
	public JSONWithPadding jsonPin(@Context UriInfo ui, JsonNode json,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		logger.info("UserResource.jsonPin");
		ApiResponse response = new ApiResponse(ui);
		response.setAction("set user pin");

		if (getUser() != null) {
			String pin = json.path("pin").getTextValue();
			management.setAppUserPin(getApplicationId(), getUserUuid(), pin);
		} else {
			response.setError("User not found");
		}

		return new JSONWithPadding(response, callback);
	}

	@GET
	@Path("resetpw")
	public Viewable showPasswordResetForm(@Context UriInfo ui,
			@QueryParam("token") String token) {

		logger.info("UserResource.showPasswordResetForm");

		this.token = token;
		try {
			if (management.checkPasswordResetTokenForAppUser(
					getApplicationId(), getUserUuid(), token)) {
				return new Viewable("resetpw_set_form", this);
			} else {
				return new Viewable("resetpw_email_form", this);
			}
		} catch (Exception e) {
			return new Viewable("error", e);
		}
	}

	@POST
	@Path("resetpw")
	@Consumes("application/x-www-form-urlencoded")
	public Viewable handlePasswordResetForm(@Context UriInfo ui,
			@FormParam("token") String token,
			@FormParam("password1") String password1,
			@FormParam("password2") String password2,
			@FormParam("recaptcha_challenge_field") String challenge,
			@FormParam("recaptcha_response_field") String uresponse) {

		try {
			logger.info("UserResource.handlePasswordResetForm");

			this.token = token;

			if ((password1 != null) || (password2 != null)) {
				if (management.checkPasswordResetTokenForAppUser(
						getApplicationId(), getUserUuid(), token)) {
					if ((password1 != null) && password1.equals(password2)) {
						management.setAppUserPassword(getApplicationId(),
								getUser().getUuid(), password1);
						return new Viewable("resetpw_set_success", this);
					} else {
						errorMsg = "Passwords didn't match, let's try again...";
						return new Viewable("resetpw_set_form", this);
					}
				} else {
					errorMsg = "Something odd happened, let's try again...";
					return new Viewable("resetpw_email_form", this);
				}
			}

			if (!useReCaptcha()) {
				management.startAppUserPasswordResetFlow(getApplicationId(),
						user);
				return new Viewable("resetpw_email_success", this);
			}

			ReCaptchaImpl reCaptcha = new ReCaptchaImpl();
			reCaptcha.setPrivateKey(properties
					.getProperty("usergrid.recaptcha.private"));

			ReCaptchaResponse reCaptchaResponse = reCaptcha.checkAnswer(
					httpServletRequest.getRemoteAddr(), challenge, uresponse);

			if (reCaptchaResponse.isValid()) {
				management.startAppUserPasswordResetFlow(getApplicationId(),
						user);
				return new Viewable("resetpw_email_success", this);
			} else {
				errorMsg = "Incorrect Captcha";
				return new Viewable("resetpw_email_form", this);
			}
		} catch (Exception e) {
			return new Viewable("error", e);
		}

	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public String getToken() {
		return token;
	}

	public User getUser() {
		if (user == null) {
			EntityManager em = getServices().getEntityManager();
			try {
				user = em.get(em.getUserByIdentifier(userIdentifier),
						User.class);
			} catch (Exception e) {
				logger.error("Unable go get user", e);
			}

		}
		return user;
	}

	public UUID getUserUuid() {
		user = getUser();
		if (user == null) {
			return null;
		}
		return user.getUuid();
	}

	@GET
	@Path("activate")
	public Viewable activate(@Context UriInfo ui,
			@QueryParam("token") String token) {

		try {
			management.handleActivationTokenForAppUser(getApplicationId(),
					getUserUuid(), token);
			return new Viewable("activate", this);
		} catch (TokenException e) {
			return new Viewable("bad_activation_token", this);
		} catch (Exception e) {
			return new Viewable("error", e);
		}
	}

	@GET
	@Path("confirm")
	public Viewable confirm(@Context UriInfo ui,
			@QueryParam("token") String token) {

		try {
			management.handleConfirmationTokenForAppUser(getApplicationId(),
					getUserUuid(), token);
			return new Viewable("activate", this);
		} catch (TokenException e) {
			return new Viewable("bad_confirmation_token", this);
		} catch (Exception e) {
			return new Viewable("error", e);
		}
	}

	@GET
	@Path("reactivate")
	public JSONWithPadding reactivate(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		logger.info("Send activation email for user: " + getUserUuid());

		ApiResponse response = new ApiResponse(ui);

		management.startAppUserActivationFlow(getApplicationId(), user);

		response.setAction("reactivate user");
		return new JSONWithPadding(response, callback);
	}

	@Override
	@Path("{itemName}")
	public AbstractContextResource addNameParameter(@Context UriInfo ui,
			@PathParam("itemName") PathSegment itemName) throws Exception {

		// check for user extension
		String resourceClass = USER_EXTENSION_RESOURCE_PREFIX
				+ StringUtils.capitalize(itemName.getPath()) + "Resource";
		AbstractUserExtensionResource extensionResource = null;
		try {
			@SuppressWarnings("unchecked")
			Class<AbstractUserExtensionResource> extensionCls = (Class<AbstractUserExtensionResource>) Class
					.forName(resourceClass);
			extensionResource = getSubResource(extensionCls);
		} catch (Exception e) {
		}
		if (extensionResource != null) {
			return extensionResource;
		}

		return super.addNameParameter(ui, itemName);
	}

}
