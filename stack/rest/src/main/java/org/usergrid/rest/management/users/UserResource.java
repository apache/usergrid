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

import static org.usergrid.security.shiro.utils.SubjectUtils.*;
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
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import net.tanesha.recaptcha.ReCaptchaImpl;
import net.tanesha.recaptcha.ReCaptchaResponse;

import org.apache.shiro.authz.AuthorizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.usergrid.management.ActivationState;
import org.usergrid.management.UserInfo;
import org.usergrid.rest.AbstractContextResource;
import org.usergrid.rest.ApiResponse;
import org.usergrid.rest.exceptions.RedirectionException;
import org.usergrid.rest.management.users.organizations.OrganizationsResource;
import org.usergrid.rest.security.annotations.RequireAdminUserAccess;
import org.usergrid.security.shiro.utils.SubjectUtils;
import org.usergrid.security.tokens.exceptions.TokenException;
import org.usergrid.services.ServiceResults;

import com.sun.jersey.api.json.JSONWithPadding;
import com.sun.jersey.api.view.Viewable;

@Component("org.usergrid.rest.management.users.UserResource")
@Scope("prototype")
@Produces({ MediaType.APPLICATION_JSON, "application/javascript",
		"application/x-javascript", "text/ecmascript",
		"application/ecmascript", "text/jscript" })
public class UserResource extends AbstractContextResource {

	private static final Logger logger = LoggerFactory
			.getLogger(UserResource.class);

	UserInfo user;

	String errorMsg;

	String token;

	public UserResource() {
	}

	public UserResource init(UserInfo user) {
		this.user = user;
		return this;
	}

	@RequireAdminUserAccess
	@Path("organizations")
	public OrganizationsResource getUserOrganizations(@Context UriInfo ui)
			throws Exception {
		return getSubResource(OrganizationsResource.class).init(user);
	}

	@RequireAdminUserAccess
	@Path("orgs")
	public OrganizationsResource getUserOrganizations2(@Context UriInfo ui)
			throws Exception {
		return getSubResource(OrganizationsResource.class).init(user);
	}

	@PUT
	public JSONWithPadding setUserInfo(@Context UriInfo ui,
			Map<String, Object> json,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		if (json == null) {
			return null;
		}

		setUserPasswordPut(ui, json, callback);

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
	public JSONWithPadding setUserPasswordPut(@Context UriInfo ui,
			Map<String, Object> json,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

        if (json == null) {
            return null;
        }

        String oldPassword = string(json.get("oldpassword"));
        String newPassword = string(json.get("newpassword"));

        if (isServiceAdmin()) {
            management.setAdminUserPassword(user.getUuid(), newPassword);
        } else {
            management.setAdminUserPassword(user.getUuid(), oldPassword,
                    newPassword);
        }

        ApiResponse response = new ApiResponse(ui);
        response.setAction("set user password");

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
			@QueryParam("token") String token) {

		try {
			this.token = token;

			if (management.checkPasswordResetTokenForAdminUser(user.getUuid(),
					token)) {
				return handleViewable("resetpw_set_form", this);
			} else {
				return handleViewable("resetpw_email_form", this);
			}
		} catch (RedirectionException e) {
			throw e;
		} catch (Exception e) {
			return handleViewable("error", e);
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
			this.token = token;

			if ((password1 != null) || (password2 != null)) {
				if (management.checkPasswordResetTokenForAdminUser(
						user.getUuid(), token)) {
					if ((password1 != null) && password1.equals(password2)) {
						management.setAdminUserPassword(user.getUuid(),
								password1);
						return handleViewable("resetpw_set_success", this);
					} else {
						errorMsg = "Passwords didn't match, let's try again...";
						return handleViewable("resetpw_set_form", this);
					}
				} else {
					errorMsg = "Something odd happened, let's try again...";
					return handleViewable("resetpw_email_form", this);
				}
			}

			if (!useReCaptcha()) {
				management.startAdminUserPasswordResetFlow(user);
				return handleViewable("resetpw_email_success", this);
			}

			ReCaptchaImpl reCaptcha = new ReCaptchaImpl();
			reCaptcha.setPrivateKey(properties
					.getProperty("usergrid.recaptcha.private"));

			ReCaptchaResponse reCaptchaResponse = reCaptcha.checkAnswer(
					httpServletRequest.getRemoteAddr(), challenge, uresponse);

			if (reCaptchaResponse.isValid()) {
				management.startAdminUserPasswordResetFlow(user);
				return handleViewable("resetpw_email_success", this);
			} else {
				errorMsg = "Incorrect Captcha";
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

	public String getToken() {
		return token;
	}

	public UserInfo getUser() {
		return user;
	}

	@GET
	@Path("activate")
	public Viewable activate(@Context UriInfo ui,
			@QueryParam("token") String token) {

		try {
			management.handleActivationTokenForAdminUser(user.getUuid(), token);
			return handleViewable("activate", this);
		} catch (TokenException e) {
			return handleViewable("bad_activation_token", this);
		} catch (RedirectionException e) {
			throw e;
		} catch (Exception e) {
			return handleViewable("error", e);
		}
	}

	@GET
	@Path("confirm")
	public Viewable confirm(@Context UriInfo ui,
			@QueryParam("token") String token) {

		try {
			ActivationState state = management
					.handleConfirmationTokenForAdminUser(user.getUuid(), token);
			if (state == ActivationState.CONFIRMED_AWAITING_ACTIVATION) {
				return handleViewable("confirm", this);
			}
			return handleViewable("activate", this);
		} catch (TokenException e) {
			return handleViewable("bad_confirmation_token", this);
		} catch (RedirectionException e) {
			throw e;
		} catch (Exception e) {
			return new Viewable("error", e);
		}
	}

	@GET
	@Path("reactivate")
	public JSONWithPadding reactivate(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		logger.info("Send activation email for user: " + user.getUuid());

		ApiResponse response = new ApiResponse(ui);

		management.startAdminUserActivationFlow(user);

		response.setAction("reactivate user");
		return new JSONWithPadding(response, callback);
	}
	
    @POST
    @Path("revoketokens")
    public JSONWithPadding revokeTokensPost(@Context UriInfo ui,
            @QueryParam("callback") @DefaultValue("callback") String callback) throws Exception {

        UUID adminId = user.getUuid();
        
        logger.info("Revoking user tokens for {}", adminId);

        ApiResponse response = new ApiResponse(ui);

        management.revokeAccessTokensForAdminUser(adminId);

        response.setAction("revoked user tokens");
        return new JSONWithPadding(response, callback);

    }

    @PUT
    @Path("revoketokens")
    public JSONWithPadding revokeTokensPut(@Context UriInfo ui,
            @QueryParam("callback") @DefaultValue("callback") String callback) throws Exception {
        return revokeTokensPost(ui, callback);

    }

}
