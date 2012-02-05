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
package org.usergrid.rest.management.organizations;

import java.util.Map;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.OrganizationOwnerInfo;
import org.usergrid.rest.AbstractContextResource;
import org.usergrid.rest.ApiResponse;
import org.usergrid.rest.management.ManagementResource;

import com.sun.jersey.api.json.JSONWithPadding;

@Produces({ MediaType.APPLICATION_JSON, "application/javascript",
		"application/x-javascript", "text/ecmascript",
		"application/ecmascript", "text/jscript" })
public class OrganizationsResource extends AbstractContextResource {

	private static final Logger logger = LoggerFactory
			.getLogger(OrganizationsResource.class);

	public OrganizationsResource(ManagementResource parent) {
		super(parent);
		logger.info("ManagementOrganizationsResource initialized");
	}

	@Path("{organizationId: [A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}}")
	public OrganizationResource getOrganizationById(@Context UriInfo ui,
			@PathParam("organizationId") String organizationIdStr)
			throws Exception {
		OrganizationInfo organization = management.getOrganizationByUuid(UUID
				.fromString(organizationIdStr));
		if (organization == null) {
			return null;
		}
		return new OrganizationResource(this, organization);
	}

	@Path("{organizationName}")
	public OrganizationResource getOrganizationByName(@Context UriInfo ui,
			@PathParam("organizationName") String organizationName)
			throws Exception {
		OrganizationInfo organization = management
				.getOrganizationByName(organizationName);
		if (organization == null) {
			return null;
		}
		return new OrganizationResource(this, organization);
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public JSONWithPadding newOrganization(@Context UriInfo ui,
			Map<String, Object> json,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {
		ApiResponse response = new ApiResponse(ui);
		response.setAction("new organization");

		String organizationName = (String) json.get("organization");
		String username = (String) json.get("username");
		String name = (String) json.get("name");
		String email = (String) json.get("email");
		String password = (String) json.get("password");

		return newOrganizationFromForm(ui, organizationName, username, name,
				email, password, callback);
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public JSONWithPadding newOrganizationFromForm(@Context UriInfo ui,
			@FormParam("organization") String organizationName,
			@FormParam("username") String username,
			@FormParam("name") String name, @FormParam("email") String email,
			@FormParam("password") String password,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		logger.info("New organization: " + organizationName);

		ApiResponse response = new ApiResponse(ui);
		response.setAction("new organization");

		OrganizationOwnerInfo organizationOwner = management
				.createOwnerAndOrganization(organizationName, username, name,
						email, password, false, false, false);
		if (organizationOwner == null) {
			return null;
		}

		management.sendOrganizationActivationEmail(organizationOwner
				.getOrganization());

		response.setData(organizationOwner);
		response.setSuccess();

		return new JSONWithPadding(response, callback);
	}

	/*
	 * @POST
	 * 
	 * @Consumes(MediaType.MULTIPART_FORM_DATA) public JSONWithPadding
	 * newOrganizationFromMultipart(@Context UriInfo ui,
	 * 
	 * @FormDataParam("organization") String organization,
	 * 
	 * @FormDataParam("username") String username,
	 * 
	 * @FormDataParam("name") String name,
	 * 
	 * @FormDataParam("email") String email,
	 * 
	 * @FormDataParam("password") String password) throws Exception { return
	 * newOrganizationFromForm(ui, organization, username, name, email,
	 * password); }
	 */
}
