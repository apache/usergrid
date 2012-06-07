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
package org.usergrid.rest.management.users.organizations;

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

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.UserInfo;
import org.usergrid.rest.AbstractContextResource;
import org.usergrid.rest.ApiResponse;
import org.usergrid.rest.security.annotations.RequireAdminUserAccess;
import org.usergrid.rest.security.annotations.RequireOrganizationAccess;
import org.usergrid.security.shiro.utils.SubjectUtils;

import com.google.common.collect.BiMap;
import com.sun.jersey.api.json.JSONWithPadding;

@Component("org.usergrid.rest.management.users.organizations.OrganizationsResource")
@Scope("prototype")
@Produces({ MediaType.APPLICATION_JSON, "application/javascript",
		"application/x-javascript", "text/ecmascript",
		"application/ecmascript", "text/jscript" })
public class OrganizationsResource extends AbstractContextResource {

	UserInfo user;

	public OrganizationsResource() {
	}

	public OrganizationsResource init(UserInfo user) {
		this.user = user;
		return this;
	}

	@RequireAdminUserAccess
	@GET
	public JSONWithPadding getUserOrganizations(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("get user management");

		BiMap<UUID, String> userOrganizations = SubjectUtils.getOrganizations();
		response.setData(userOrganizations.inverse());

		return new JSONWithPadding(response, callback);
	}

	@RequireAdminUserAccess
	@POST
	public JSONWithPadding newOrganizationForUser(@Context UriInfo ui,
			Map<String, Object> json,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("new organization for user");

		String organizationName = (String) json.get("organization");
		OrganizationInfo organization = management.createOrganization(
				organizationName, user, false, true);
		response.setData(organization);

		management.activateOrganization(organization);

		return new JSONWithPadding(response, callback);
	}

	@RequireAdminUserAccess
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public JSONWithPadding newOrganizationForUserFromForm(@Context UriInfo ui,
			Map<String, Object> json,
			@QueryParam("callback") @DefaultValue("callback") String callback,
			@FormParam("organization") String organizationName)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("new organization for user");

		if (organizationName == null) {
			return null;
		}

		OrganizationInfo organization = management.createOrganization(
				organizationName, user, false, true);
		response.setData(organization);

		management.activateOrganization(organization);

		return new JSONWithPadding(response, callback);
	}

	@RequireOrganizationAccess
	@PUT
	@Path("{organizationName}")
	public JSONWithPadding addUserToOrganizationByOrganizationName(
			@Context UriInfo ui,
			@PathParam("organizationName") String organizationName,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("add user to organization");

		OrganizationInfo organization = management
				.getOrganizationByName(organizationName);
		management.addAdminUserToOrganization(user, organization, true);
		response.setData(organization);
		return new JSONWithPadding(response, callback);
	}

	@RequireOrganizationAccess
	@PUT
	@Path("{organizationId: [A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}}")
	public JSONWithPadding addUserToOrganizationByOrganizationId(
			@Context UriInfo ui,
			@PathParam("organizationId") String organizationIdStr,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("add user to organization");

		OrganizationInfo organization = management.getOrganizationByUuid(UUID
				.fromString(organizationIdStr));
		management.addAdminUserToOrganization(user, organization, true);
		response.setData(organization);
		return new JSONWithPadding(response, callback);
	}

	@RequireOrganizationAccess
	@DELETE
	@Path("{organizationId: [A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}}")
	public JSONWithPadding removeUserFromOrganizationByOrganizationId(
			@Context UriInfo ui,
			@PathParam("organizationId") String organizationIdStr,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("remove user from organization");

		OrganizationInfo organization = management.getOrganizationByUuid(UUID
				.fromString(organizationIdStr));
		management.removeAdminUserFromOrganization(user.getUuid(),
				organization.getUuid());
		response.setData(organization);
		return new JSONWithPadding(response, callback);
	}

	@RequireOrganizationAccess
	@DELETE
	@Path("{organizationName}")
	public JSONWithPadding removeUserFromOrganizationByOrganizationName(
			@Context UriInfo ui,
			@PathParam("organizationName") String organizationName,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("remove user from organization");
		OrganizationInfo organization = management
				.getOrganizationByName(organizationName);
		management.removeAdminUserFromOrganization(user.getUuid(),
				organization.getUuid());
		response.setData(organization);

		return new JSONWithPadding(response, callback);
	}

}
