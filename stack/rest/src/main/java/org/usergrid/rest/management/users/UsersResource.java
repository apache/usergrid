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
package org.usergrid.rest.management.users;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.usergrid.rest.exceptions.SecurityException.mappableSecurityException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import net.tanesha.recaptcha.ReCaptchaImpl;
import net.tanesha.recaptcha.ReCaptchaResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.usergrid.management.UserInfo;
import org.usergrid.rest.AbstractContextResource;
import org.usergrid.rest.ApiResponse;
import org.usergrid.rest.exceptions.AuthErrorInfo;

import com.sun.jersey.api.json.JSONWithPadding;
import com.sun.jersey.api.view.Viewable;

@Path("/management/users")
@Component
@Scope("singleton")
@Produces({ MediaType.APPLICATION_JSON, "application/javascript",
		"application/x-javascript", "text/ecmascript",
		"application/ecmascript", "text/jscript" })
public class UsersResource extends AbstractContextResource {

	private static final Logger logger = LoggerFactory
			.getLogger(UsersResource.class);

	String errorMsg;
	UserInfo user;

	public UsersResource() {
		logger.info("ManagementUsersResource initialized");
	}

	@Path("{userId: [A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}}")
	public UserResource getUserById(@Context UriInfo ui,
			@PathParam("userId") String userIdStr) throws Exception {

		return new UserResource(this, management.getAdminUserByUuid(UUID
				.fromString(userIdStr)));
	}

	@Path("{username}")
	public UserResource getUserByUsername(@Context UriInfo ui,
			@PathParam("username") String username) throws Exception {

		return new UserResource(this,
				management.getAdminUserByUsername(username));
	}

	@Path("{email: [a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}}")
	public UserResource getUserByEmail(@Context UriInfo ui,
			@PathParam("email") String email) throws Exception {

		return new UserResource(this, management.getAdminUserByEmail(email));
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public JSONWithPadding createUser(@Context UriInfo ui,
			@FormParam("username") String username,
			@FormParam("name") String name, @FormParam("email") String email,
			@FormParam("password") String password,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		logger.info("Create user: " + username);

		ApiResponse response = new ApiResponse(ui);
		response.setAction("create user");

		UserInfo user = management.createAdminUser(username, name, email,
				password, false, false, false);
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		if (user != null) {
			management.sendAdminUserActivationEmail(user);
			result.put("user", user);
			response.setData(result);
			response.setSuccess();
		} else {
			throw mappableSecurityException(AuthErrorInfo.BAD_CREDENTIALS_SYNTAX_ERROR);
		}

		return new JSONWithPadding(response, callback);
	}

	/*
	 * @POST
	 * 
	 * @Consumes(MediaType.MULTIPART_FORM_DATA) public JSONWithPadding
	 * createUserFromMultipart(@Context UriInfo ui,
	 * 
	 * @FormDataParam("username") String username,
	 * 
	 * @FormDataParam("name") String name,
	 * 
	 * @FormDataParam("email") String email,
	 * 
	 * @FormDataParam("password") String password) throws Exception {
	 * 
	 * return createUser(ui, username, name, email, password); }
	 */

	@GET
	@Path("resetpw")
	public Viewable showPasswordResetForm(@Context UriInfo ui) throws Exception {
		return new Viewable("resetpw_email_form", this);
	}

	@POST
	@Path("resetpw")
	@Consumes("application/x-www-form-urlencoded")
	public Viewable handlePasswordResetForm(@Context UriInfo ui,
			@FormParam("email") String email,
			@FormParam("recaptcha_challenge_field") String challenge,
			@FormParam("recaptcha_response_field") String uresponse)
			throws Exception {

		if (isBlank(email)) {
			errorMsg = "No email provided, try again...";
			return new Viewable("resetpw_email_form", this);
		}

		ReCaptchaImpl reCaptcha = new ReCaptchaImpl();
		reCaptcha.setPrivateKey(properties
				.getProperty("usergrid.recaptcha.private"));

		ReCaptchaResponse reCaptchaResponse = reCaptcha.checkAnswer(
				httpServletRequest.getRemoteAddr(), challenge, uresponse);

		if (!useReCaptcha() || reCaptchaResponse.isValid()) {
			user = management.getAdminUserByEmail(email);
			if (user != null) {
				management.sendAdminUserPasswordReminderEmail(user);
				return new Viewable("resetpw_email_success", this);
			} else {
				errorMsg = "We don't recognize that email, try again...";
				return new Viewable("resetpw_email_form", this);
			}
		} else {
			errorMsg = "Incorrect Captcha, try again...";
			return new Viewable("resetpw_email_form", this);
		}

	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public UserInfo getUser() {
		return user;
	}

}
