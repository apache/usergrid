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

import static org.usergrid.utils.ConversionUtils.string;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import net.tanesha.recaptcha.ReCaptchaImpl;
import net.tanesha.recaptcha.ReCaptchaResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.management.UserInfo;
import org.usergrid.rest.AbstractContextResource;
import org.usergrid.rest.ApiResponse;
import org.usergrid.rest.management.users.organizations.OrganizationsResource;
import org.usergrid.rest.security.annotations.RequireAdminUserAccess;
import org.usergrid.security.shiro.utils.SubjectUtils;
import org.usergrid.services.ServiceResults;

import com.sun.jersey.api.json.JSONWithPadding;
import com.sun.jersey.api.view.Viewable;

@Produces({ MediaType.APPLICATION_JSON, "application/javascript",
		"application/x-javascript", "text/ecmascript",
		"application/ecmascript", "text/jscript" })
public class UserResource extends AbstractContextResource {

	private static final Logger logger = LoggerFactory
			.getLogger(UserResource.class);

	UserInfo user;

	String errorMsg;

	String token;

	public UserResource(AbstractContextResource parent, UserInfo user) {
		super(parent);
		this.user = user;
	}

	@RequireAdminUserAccess
	@Path("organizations")
	public OrganizationsResource getUserOrganizations(@Context UriInfo ui)
			throws Exception {
		return new OrganizationsResource(this, user);
	}

	@PUT
	public JSONWithPadding setUserInfo(@Context UriInfo ui,
			Map<String, Object> json,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		if (json == null) {
			return null;
		}

		String oldPassword = string(json.get("oldpassword"));
		String newPassword = string(json.get("newpassword"));
		if ((oldPassword != null) && (newPassword != null)) {
			management.setAdminUserPassword(user.getUuid(), oldPassword,
					newPassword);
		}

		String email = string(json.get("email"));
		String username = string(json.get("username"));
		String name = string(json.get("name"));

		management.updateAdminUser(user, username, name, email);

		ApiResponse response = new ApiResponse(ui);
		response.setAction("update user info");

		return new JSONWithPadding(response, callback);
	}

	@PUT
	@Path("password")
	public JSONWithPadding setUserPassword(@Context UriInfo ui,
			Map<String, Object> json,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		if (json == null) {
			return null;
		}

		String oldPassword = string(json.get("oldpassword"));
		String newPassword = string(json.get("newpassword"));
		management.setAdminUserPassword(user.getUuid(), oldPassword,
				newPassword);

		ApiResponse response = new ApiResponse(ui);
		response.setAction("set user password");

		return new JSONWithPadding(response, callback);
	}

	@RequireAdminUserAccess
	@GET
	@Path("feed")
	public JSONWithPadding getFeed(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("get admin user feed");

		ServiceResults results = management.getAdminUserActivity(user);
		response.setEntities(results.getEntities());
		response.setSuccess();

		return new JSONWithPadding(response, callback);
	}

	@RequireAdminUserAccess
	@GET
	public JSONWithPadding getUserData(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("get admin user");

		String token = management.getAccessTokenForAdminUser(SubjectUtils
				.getUser().getUuid());
		Map<String, Object> userOrganizationData = management
				.getAdminUserOrganizationData(user.getUuid());
		userOrganizationData.put("token", token);
		response.setData(userOrganizationData);
		response.setSuccess();

		return new JSONWithPadding(response, callback);
	}

	@GET
	@Path("resetpw")
	public Viewable showPasswordResetForm(@Context UriInfo ui,
			@QueryParam("token") String token) throws Exception {

		this.token = token;

		if (management.checkPasswordResetTokenForAdminUser(user.getUuid(),
				token)) {
			return new Viewable("resetpw_set_form", this);
		} else {
			return new Viewable("resetpw_email_form", this);
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
			@FormParam("recaptcha_response_field") String uresponse)
			throws Exception {

		this.token = token;

		if ((password1 != null) || (password2 != null)) {
			if (management.checkPasswordResetTokenForAdminUser(user.getUuid(),
					token)) {
				if ((password1 != null) && password1.equals(password2)) {
					management.setAdminUserPassword(user.getUuid(), password1);
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
			management.sendAdminUserPasswordReminderEmail(user);
			return new Viewable("resetpw_email_success", this);
		}

		ReCaptchaImpl reCaptcha = new ReCaptchaImpl();
		reCaptcha.setPrivateKey(properties
				.getProperty("usergrid.recaptcha.private"));

		ReCaptchaResponse reCaptchaResponse = reCaptcha.checkAnswer(
				httpServletRequest.getRemoteAddr(), challenge, uresponse);

		if (reCaptchaResponse.isValid()) {
			management.sendAdminUserPasswordReminderEmail(user);
			return new Viewable("resetpw_email_success", this);
		} else {
			errorMsg = "Incorrect Captcha";
			return new Viewable("resetpw_email_form", this);
		}

	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public String getToken() {
		return token;
	}

	public UserInfo getUser() {
		return user;
	}

	@GET
	@Path("activate")
	public Viewable activate(@Context UriInfo ui,
			@QueryParam("token") String token,
			@QueryParam("confirm") boolean confirm) throws Exception {

		if (management.checkActivationTokenForAdminUser(user.getUuid(), token)) {
			management.activateAdminUser(user.getUuid());
			if (confirm) {
				management.sendAdminUserActivatedEmail(user);
			}
			return new Viewable("activate", this);
		}
		return null;
	}

	@GET
	@Path("reactivate")
	public JSONWithPadding reactivate(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		logger.info("Send activation email for user: " + user.getUuid());

		ApiResponse response = new ApiResponse(ui);

		management.sendAdminUserActivationEmail(user);

		response.setAction("reactivate user");
		return new JSONWithPadding(response, callback);
	}

}
