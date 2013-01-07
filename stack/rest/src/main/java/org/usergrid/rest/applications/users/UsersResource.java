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

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.usergrid.services.ServiceParameter.addParameter;

import java.util.List;
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
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import net.tanesha.recaptcha.ReCaptchaImpl;
import net.tanesha.recaptcha.ReCaptchaResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.Identifier;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.entities.User;
import org.usergrid.rest.AbstractContextResource;
import org.usergrid.rest.ApiResponse;
import org.usergrid.rest.applications.ServiceResource;
import org.usergrid.rest.exceptions.RedirectionException;
import org.usergrid.rest.security.annotations.RequireApplicationAccess;

import com.sun.jersey.api.json.JSONWithPadding;
import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.core.provider.EntityHolder;

@Component("org.usergrid.rest.applications.users.UsersResource")
@Scope("prototype")
@Produces(MediaType.APPLICATION_JSON)
public class UsersResource extends ServiceResource {

	private static final Logger logger = LoggerFactory
			.getLogger(UsersResource.class);

	String errorMsg;
	User user;

	public UsersResource() {
	}

	@Override
	@Path("{entityId: [A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}}")
	public AbstractContextResource addIdParameter(@Context UriInfo ui,
			@PathParam("entityId") PathSegment entityId) throws Exception {

		logger.info("ServiceResource.addIdParameter");

		UUID itemId = UUID.fromString(entityId.getPath());

		addParameter(getServiceParameters(), itemId);

		addMatrixParams(getServiceParameters(), ui, entityId);

		return getSubResource(UserResource.class).init(
				Identifier.fromUUID(itemId));
	}

	@Override
	@Path("{itemName}")
	public AbstractContextResource addNameParameter(@Context UriInfo ui,
			@PathParam("itemName") PathSegment itemName) throws Exception {

		logger.info("ServiceResource.addNameParameter");

		logger.info("Current segment is " + itemName.getPath());

		if (itemName.getPath().startsWith("{")) {
			Query query = Query.fromJsonString(itemName.getPath());
			if (query != null) {
				addParameter(getServiceParameters(), query);
			}
			addMatrixParams(getServiceParameters(), ui, itemName);

			return getSubResource(ServiceResource.class);
		}

		addParameter(getServiceParameters(), itemName.getPath());

		addMatrixParams(getServiceParameters(), ui, itemName);
    Identifier id = Identifier.from(itemName.getPath());
    if ( id == null ) {
      throw new IllegalArgumentException("Not a valid user identifier: " + itemName.getPath());
    }
		return getSubResource(UserResource.class).init(id);
	}

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
			ReCaptchaImpl reCaptcha = new ReCaptchaImpl();
			reCaptcha.setPrivateKey(properties.getRecaptchaPrivate());

			ReCaptchaResponse reCaptchaResponse = reCaptcha.checkAnswer(
					httpServletRequest.getRemoteAddr(), challenge, uresponse);

			if (isBlank(email)) {
				errorMsg = "No email provided, try again...";
				return handleViewable("resetpw_email_form", this);
			}

			if (!useReCaptcha() || reCaptchaResponse.isValid()) {
				user = management.getAppUserByIdentifier(getApplicationId(),
						Identifier.fromEmail(email));
				if (user != null) {
					management.startAppUserPasswordResetFlow(
							getApplicationId(), user);
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
			return handleViewable("resetpw_email_form", e);
		}

	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public User getUser() {
		return user;
	}

	@POST
	@Override
	@RequireApplicationAccess
	public JSONWithPadding executePost(@Context UriInfo ui,
			EntityHolder<Object> body,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {
		Object json = body.getEntity();
		String password = null;
		String pin = null;

		Boolean registration_requires_email_confirmation = (Boolean) this
				.getServices()
				.getEntityManager()
				.getProperty(this.getServices().getApplicationRef(),
						"registration_requires_email_confirmation");
		boolean activated = !((registration_requires_email_confirmation != null) && registration_requires_email_confirmation);

		if (json instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) json;
			password = (String) map.get("password");
			map.remove("password");
			pin = (String) map.get("pin");
			map.remove("pin");
			map.put("activated", activated);
		} else if (json instanceof List) {
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>) json;
			for (Object obj : list) {
				if (obj instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, Object> map = (Map<String, Object>) obj;
					map.remove("password");
					map.remove("pin");
				}
			}
		}

		ApiResponse response = (ApiResponse) super.executePost(ui, body,
				callback).getJsonSource();

		if ((response.getEntities() != null)
				&& (response.getEntities().size() == 1)) {

			Entity entity = response.getEntities().get(0);
			User user = (User)entity.toTypedEntity();

			if (isNotBlank(password)) {
				management.setAppUserPassword(getApplicationId(),
						user.getUuid(), password);
			}

			if (isNotBlank(pin)) {
				management.setAppUserPin(getApplicationId(), user.getUuid(),
						pin);
			}

			if (!activated) {
				management.startAppUserActivationFlow(getApplicationId(),
						user);
			}
		}
		return new JSONWithPadding(response, callback);
	}

}
