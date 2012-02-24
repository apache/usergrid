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
package org.usergrid.rest.management.organizations.applications;

import static org.apache.commons.lang.StringUtils.isEmpty;

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

import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.rest.AbstractContextResource;
import org.usergrid.rest.ApiResponse;
import org.usergrid.rest.security.annotations.RequireOrganizationAccess;

import com.google.common.collect.BiMap;
import com.sun.jersey.api.json.JSONWithPadding;

@Produces({ MediaType.APPLICATION_JSON, "application/javascript",
		"application/x-javascript", "text/ecmascript",
		"application/ecmascript", "text/jscript" })
public class ApplicationsResource extends AbstractContextResource {

	OrganizationInfo organization;

	public ApplicationsResource() {
	}

	public ApplicationsResource init(OrganizationInfo organization) {
		this.organization = organization;
		return this;
	}

	@RequireOrganizationAccess
	@GET
	public JSONWithPadding getOrganizationApplications(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("get organization application");

		BiMap<UUID, String> applications = management
				.getApplicationsForOrganization(organization.getUuid());
		response.setData(applications.inverse());

		return new JSONWithPadding(response, callback);
	}

	@RequireOrganizationAccess
	@POST
	public JSONWithPadding newApplicationForOrganization(@Context UriInfo ui,
			Map<String, Object> json,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		String applicationName = (String) json.get("name");
		if (isEmpty(applicationName)) {
			return null;
		}

		ApiResponse response = new ApiResponse(ui);
		response.setAction("new application for organization");

		UUID applicationId = management.createApplication(
				organization.getUuid(), applicationName);

		LinkedHashMap<String, UUID> applications = new LinkedHashMap<String, UUID>();
		applications.put(applicationName, applicationId);
		response.setData(applications);

		return new JSONWithPadding(response, callback);
	}

	@RequireOrganizationAccess
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public JSONWithPadding newApplicationForOrganizationFromForm(
			@Context UriInfo ui, Map<String, Object> json,
			@QueryParam("callback") @DefaultValue("callback") String callback,
			@FormParam("name") String applicationName) throws Exception {

		if (isEmpty(applicationName)) {
			return null;
		}

		ApiResponse response = new ApiResponse(ui);
		response.setAction("new application for organization");

		UUID applicationId = management.createApplication(
				organization.getUuid(), applicationName);

		LinkedHashMap<String, UUID> applications = new LinkedHashMap<String, UUID>();
		applications.put(applicationName, applicationId);
		response.setData(applications);

		return new JSONWithPadding(response, callback);
	}

	@RequireOrganizationAccess
	@Path("{applicationId: [A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}}")
	public ApplicationResource deleteApplicationFromOrganizationByApplicationId(
			@Context UriInfo ui,
			@PathParam("applicationId") String applicationIdStr)
			throws Exception {

		return getSubResource(ApplicationResource.class).init(organization,
				UUID.fromString(applicationIdStr));
	}

	@RequireOrganizationAccess
	@Path("{applicationName}")
	public ApplicationResource deleteApplicationFromOrganizationByApplicationName(
			@Context UriInfo ui,
			@PathParam("applicationName") String applicationName)
			throws Exception {

		ApplicationInfo application = management
				.getApplication(applicationName);

		return getSubResource(ApplicationResource.class).init(organization,
				application);
	}

}
