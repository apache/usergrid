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
import org.springframework.stereotype.Component;
import org.usergrid.management.UserInfo;
import org.usergrid.rest.AbstractContextResource;
import org.usergrid.rest.ApiResponse;
import org.usergrid.rest.exceptions.AuthErrorInfo;
import org.usergrid.rest.exceptions.RedirectionException;
import org.usergrid.security.shiro.utils.SubjectUtils;

import com.sun.jersey.api.json.JSONWithPadding;
import com.sun.jersey.api.view.Viewable;

@Component("org.usergrid.rest.management.users.UsersResource")
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

		return getSubResource(UserResource.class).init(
				management.getAdminUserByUuid(UUID.fromString(userIdStr)));
	}

	@Path("{username}")
	public UserResource getUserByUsername(@Context UriInfo ui,
			@PathParam("username") String username) throws Exception {

	    if ("me".equals(username)) {
	        UserInfo user = SubjectUtils.getAdminUser();
	        if ((user != null) && (user.getUuid() != null)) {
	            return getSubResource(UserResource.class).init(
	                    management.getAdminUserByUuid(user.getUuid()));
	        }
	        throw mappableSecurityException("unauthorized",
	                "No admin identity for access credentials provided");
	    }

		return getSubResource(UserResource.class).init(
				management.getAdminUserByUsername(username));
	}

	@Path("{email: [a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}}")
	public UserResource getUserByEmail(@Context UriInfo ui,
			@PathParam("email") String email) throws Exception {

		return getSubResource(UserResource.class).init(
				management.getAdminUserByEmail(email));
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

		ApiResponse response = createApiResponse();
		response.setAction("create user");

		UserInfo user = management.createAdminUser(username, name, email,
				password, false, false);
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		if (user != null) {
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
	public Viewable showPasswordResetForm(@Context UriInfo ui) {
		return handleViewable("resetpw_email_form", this);
	}

	@POST
	@Path("resetpw")
	@Consumes("application/x-www-form-urlencoded")
	public Viewable handlePasswordResetForm(@Context UriInfo ui,
			@FormParam("email") String email,
			@FormParam("recaptcha_challenge_field") String challenge,
			@FormParam("recaptcha_response_field") String uresponse) {

		try {
			if (isBlank(email)) {
				errorMsg = "No email provided, try again...";
				return handleViewable("resetpw_email_form", this);
			}

			ReCaptchaImpl reCaptcha = new ReCaptchaImpl();
			reCaptcha.setPrivateKey(properties.getRecaptchaPrivate());

			ReCaptchaResponse reCaptchaResponse = reCaptcha.checkAnswer(
					httpServletRequest.getRemoteAddr(), challenge, uresponse);

			if (!useReCaptcha() || reCaptchaResponse.isValid()) {
				user = management.findAdminUser(email);
				if (user != null) {
					management.startAdminUserPasswordResetFlow(user);
					return handleViewable("resetpw_email_success", this);
				} else {
					errorMsg = "We don't recognize that email, try again...";
					return handleViewable("resetpw_email_form", this);
				}
			} else {
				errorMsg = "Incorrect Captcha, try again...";
				return handleViewable("resetpw_email_form", this);
			}
		} catch (RedirectionException e) {
			throw e;
		} catch (Exception e) {
			return handleViewable("error", e);
		}

	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public UserInfo getUser() {
		return user;
	}

}
