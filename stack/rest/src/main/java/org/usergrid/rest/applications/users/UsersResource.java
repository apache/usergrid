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
package org.usergrid.rest.applications.users;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.usergrid.services.ServiceParameter.addParameter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import net.tanesha.recaptcha.ReCaptchaImpl;
import net.tanesha.recaptcha.ReCaptchaResponse;

import org.apache.log4j.Logger;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.Identifier;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.entities.User;
import org.usergrid.rest.ApiResponse;
import org.usergrid.rest.applications.ServiceResource;

import com.sun.jersey.api.view.Viewable;

@Produces(MediaType.APPLICATION_JSON)
public class UsersResource extends ServiceResource {

	private static final Logger logger = Logger.getLogger(UsersResource.class);

	String errorMsg;
	User user;

	public UsersResource(ServiceResource parent) throws Exception {
		super(parent);
	}

	@Override
	@Path("{entityId: [A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}}")
	public ServiceResource addIdParameter(@Context UriInfo ui,
			@PathParam("entityId") PathSegment entityId) throws Exception {

		logger.info("ServiceResource.addIdParameter");

		UUID itemId = UUID.fromString(entityId.getPath());

		addParameter(getServiceParameters(), itemId);

		addMatrixParams(getServiceParameters(), ui, entityId);

		return new UserResource(this, Identifier.fromUUID(itemId));
	}

	@Override
	@Path("{itemName}")
	public ServiceResource addNameParameter(@Context UriInfo ui,
			@PathParam("itemName") PathSegment itemName) throws Exception {

		logger.info("ServiceResource.addNameParameter");

		logger.info("Current segment is " + itemName.getPath());

		if (itemName.getPath().startsWith("{")) {
			Query query = Query.fromJsonString(itemName.getPath());
			if (query != null) {
				addParameter(getServiceParameters(), query);
			}
			addMatrixParams(getServiceParameters(), ui, itemName);

			return new ServiceResource(this);
		}

		addParameter(getServiceParameters(), itemName.getPath());

		addMatrixParams(getServiceParameters(), ui, itemName);

		return new UserResource(this, Identifier.from(itemName.getPath()));
	}

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

		ReCaptchaImpl reCaptcha = new ReCaptchaImpl();
		reCaptcha.setPrivateKey(properties
				.getProperty("usergrid.recaptcha.private"));

		ReCaptchaResponse reCaptchaResponse = reCaptcha.checkAnswer(
				httpServletRequest.getRemoteAddr(), challenge, uresponse);

		if ((email != null) && reCaptchaResponse.isValid()) {
			user = management.getAppUserByIdentifier(getApplicationId(),
					Identifier.fromEmail(email));
			if (user != null) {
				management.sendAppUserPasswordReminderEmail(getApplicationId(),
						user);
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

	public User getUser() {
		return user;
	}

	@POST
	@Override
	public ApiResponse executePost(@Context UriInfo ui, Object json)
			throws Exception {
		String password = null;
		String pin = null;
		if (json instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) json;
			password = (String) map.get("password");
			map.remove("password");
			pin = (String) map.get("pin");
			map.remove("pin");
			map.put("activated", true);
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

		ApiResponse response = super.executePost(ui, json);

		if ((response.getEntities() != null)
				&& (response.getEntities().size() == 1)) {

			Entity user = response.getEntities().get(0);

			if (isNotBlank(password)) {
				management.setAppUserPassword(getApplicationId(),
						user.getUuid(), password);
			}

			if (isNotBlank(pin)) {
				management.setAppUserPin(getApplicationId(), user.getUuid(),
						pin);
			}
		}
		return response;
	}

}
