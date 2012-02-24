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
package org.usergrid.rest.management.organizations.users;

import static org.apache.commons.collections.MapUtils.getObject;
import static org.usergrid.utils.ConversionUtils.getBoolean;
import static org.usergrid.utils.ConversionUtils.string;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.UserInfo;
import org.usergrid.rest.AbstractContextResource;
import org.usergrid.rest.ApiResponse;
import org.usergrid.rest.security.annotations.RequireOrganizationAccess;

import com.sun.jersey.api.json.JSONWithPadding;

@Component("org.usergrid.rest.management.organizations.users.UsersResource")
@Scope("prototype")
@Produces({ MediaType.APPLICATION_JSON, "application/javascript",
		"application/x-javascript", "text/ecmascript",
		"application/ecmascript", "text/jscript" })
public class UsersResource extends AbstractContextResource {

	private static final Logger logger = LoggerFactory
			.getLogger(UsersResource.class);

	OrganizationInfo organization;

	public UsersResource() {
	}

	public UsersResource init(OrganizationInfo organization) {
		this.organization = organization;
		return this;
	}

	@RequireOrganizationAccess
	@GET
	public JSONWithPadding getOrganizationUsers(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("get organization users");

		List<UserInfo> users = management
				.getAdminUsersForOrganization(organization.getUuid());
		response.setData(users);
		return new JSONWithPadding(response, callback);
	}

	@RequireOrganizationAccess
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public JSONWithPadding newUserForOrganization(@Context UriInfo ui,
			Map<String, Object> json,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("new user for organization");

		String email = string(json.get("email"));
		String password = string(json.get("password"));
		boolean invite = getBoolean(getObject(json, "invite", true));

		UserInfo user = null;
		if (invite) {
			user = management.getAdminUserByEmail(email);
		}

		if (user == null) {
			user = management.createAdminUser(email, email, email, password,
					false, false, true);
		}

		if (user == null) {
			return null;
		}

		management.addAdminUserToOrganization(user.getUuid(),
				organization.getUuid());

		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("user", user);
		response.setData(result);
		response.setSuccess();

		return new JSONWithPadding(response, callback);
	}

	@RequireOrganizationAccess
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public JSONWithPadding newUserForOrganizationFromForm(@Context UriInfo ui,
			@FormParam("username") String username,
			@FormParam("name") String name, @FormParam("email") String email,
			@FormParam("password") String password,
			@FormParam("invite") @DefaultValue("true") boolean invite,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		logger.info("New user for organization: " + username);

		ApiResponse response = new ApiResponse(ui);
		response.setAction("create user");

		UserInfo user = null;
		if (invite) {
			user = management.getAdminUserByEmail(email);
		}

		if (user == null) {
			user = management.createAdminUser(email, email, email, password,
					false, false, true);
		}

		if (user == null) {
			return null;
		}

		management.addAdminUserToOrganization(user.getUuid(),
				organization.getUuid());

		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("user", user);
		response.setData(result);
		response.setSuccess();

		return new JSONWithPadding(response, callback);
	}

	/*
	 * @RequireOrganizationAccess
	 * 
	 * @POST
	 * 
	 * @Consumes(MediaType.MULTIPART_FORM_DATA) public JSONWithPadding
	 * newUserForOrganizationFromMultipart(
	 * 
	 * @Context UriInfo ui, @FormDataParam("username") String username,
	 * 
	 * @FormDataParam("name") String name,
	 * 
	 * @FormDataParam("email") String email,
	 * 
	 * @FormDataParam("password") String password) throws Exception {
	 * 
	 * return newUserForOrganizationFromForm(ui, username, name, email,
	 * password); }
	 */

	@RequireOrganizationAccess
	@PUT
	@Path("{userId: [A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}}")
	public JSONWithPadding addUserToOrganization(@Context UriInfo ui,
			@PathParam("userId") String userIdStr,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("add user to organization");

		UserInfo user = management.getAdminUserByUuid(UUID
				.fromString(userIdStr));
		if (user == null) {
			return null;
		}
		management.addAdminUserToOrganization(user.getUuid(),
				organization.getUuid());

		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("user", user);
		response.setData(result);
		response.setSuccess();

		return new JSONWithPadding(response, callback);
	}

	@RequireOrganizationAccess
	@PUT
	@Path("{username}")
	public JSONWithPadding addUserToOrganizationByUsername(@Context UriInfo ui,
			@PathParam("username") String username,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("add user to organization");

		UserInfo user = management.getAdminUserByUsername(username);
		if (user == null) {
			return null;
		}
		management.addAdminUserToOrganization(user.getUuid(),
				organization.getUuid());

		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("user", user);
		response.setData(result);
		response.setSuccess();

		return new JSONWithPadding(response, callback);
	}

	@RequireOrganizationAccess
	@PUT
	@Path("{email: [A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}}")
	public JSONWithPadding addUserToOrganizationByEmail(@Context UriInfo ui,
			@PathParam("email") String email,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("add user to organization");

		UserInfo user = management.getAdminUserByEmail(email);
		if (user == null) {
			return null;
		}
		management.addAdminUserToOrganization(user.getUuid(),
				organization.getUuid());

		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("user", user);
		response.setData(result);
		response.setSuccess();

		return new JSONWithPadding(response, callback);
	}

	@RequireOrganizationAccess
	@DELETE
	@Path("{userId: [A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}}")
	public JSONWithPadding removeUserFromOrganizationByUserId(
			@Context UriInfo ui, @PathParam("userId") String userIdStr,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("remove user from organization");

		UserInfo user = management.getAdminUserByUuid(UUID
				.fromString(userIdStr));
		if (user == null) {
			return null;
		}
		management.removeAdminUserFromOrganization(user.getUuid(),
				organization.getUuid());

		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("user", user);
		response.setData(result);
		response.setSuccess();

		return new JSONWithPadding(response, callback);
	}

	@RequireOrganizationAccess
	@DELETE
	@Path("{username}")
	public JSONWithPadding removeUserFromOrganizationByUsername(
			@Context UriInfo ui, @PathParam("username") String username,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("remove user from organization");

		UserInfo user = management.getAdminUserByUsername(username);
		if (user == null) {
			return null;
		}
		management.removeAdminUserFromOrganization(user.getUuid(),
				organization.getUuid());

		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("user", user);
		response.setData(result);
		response.setSuccess();

		return new JSONWithPadding(response, callback);
	}

	@RequireOrganizationAccess
	@DELETE
	@Path("{email: [A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}}")
	public JSONWithPadding removeUserFromOrganizationByEmail(
			@Context UriInfo ui, @PathParam("email") String email,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("remove user from organization");

		UserInfo user = management.getAdminUserByEmail(email);
		if (user == null) {
			return null;
		}
		management.removeAdminUserFromOrganization(user.getUuid(),
				organization.getUuid());

		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("user", user);
		response.setData(result);
		response.setSuccess();

		return new JSONWithPadding(response, callback);
	}
}
