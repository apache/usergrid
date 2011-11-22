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

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.rest.AbstractContextResource;
import org.usergrid.rest.ApiResponse;
import org.usergrid.rest.management.organizations.applications.ApplicationsResource;
import org.usergrid.rest.management.organizations.users.UsersResource;
import org.usergrid.rest.security.annotations.RequireOrganizationAccess;
import org.usergrid.security.oauth.ClientCredentialsInfo;
import org.usergrid.services.ServiceResults;

import com.sun.jersey.api.json.JSONWithPadding;
import com.sun.jersey.api.view.Viewable;

@Produces({ MediaType.APPLICATION_JSON, "application/javascript",
		"application/x-javascript", "text/ecmascript",
		"application/ecmascript", "text/jscript" })
public class OrganizationResource extends AbstractContextResource {

	private static final Logger logger = LoggerFactory
			.getLogger(OrganizationsResource.class);

	OrganizationInfo organization;

	public OrganizationResource(AbstractContextResource parent,
			OrganizationInfo organization) {
		super(parent);
		this.organization = organization;
	}

	@RequireOrganizationAccess
	@Path("users")
	public UsersResource getOrganizationUsers(@Context UriInfo ui)
			throws Exception {
		return new UsersResource(this, organization);
	}

	@RequireOrganizationAccess
	@Path("applications")
	public ApplicationsResource getOrganizationApplications(@Context UriInfo ui)
			throws Exception {
		return new ApplicationsResource(this, organization);
	}

	@GET
	@Path("activate")
	public Viewable activate(@Context UriInfo ui,
			@QueryParam("token") String token,
			@QueryParam("confirm") boolean confirm) throws Exception {

		if (management.checkActivationTokenForOrganization(
				organization.getUuid(), token)) {
			management.activateOrganization(organization.getUuid());
			if (confirm) {
				management.sendOrganizationActivatedEmail(organization);
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

		logger.info("Send activation email for organization: "
				+ organization.getUuid());

		ApiResponse response = new ApiResponse(ui);

		management.sendOrganizationActivationEmail(organization);

		response.setAction("reactivate organization");
		return new JSONWithPadding(response, callback);
	}

	@RequireOrganizationAccess
	@GET
	@Path("feed")
	public JSONWithPadding getFeed(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("get organization feed");

		ServiceResults results = management
				.getOrganizationActivity(organization);
		response.setEntities(results.getEntities());
		response.setSuccess();

		return new JSONWithPadding(response, callback);
	}

	@RequireOrganizationAccess
	@GET
	@Path("credentials")
	public JSONWithPadding getCredentials(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("get organization client credentials");

		ClientCredentialsInfo keys = new ClientCredentialsInfo(
				management.getClientIdForOrganization(organization.getUuid()),
				management.getClientSecretForOrganization(organization
						.getUuid()));

		response.setCredentials(keys);
		return new JSONWithPadding(response, callback);
	}

	@RequireOrganizationAccess
	@POST
	@Path("credentials")
	public JSONWithPadding generateCredentials(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("generate organization client credentials");

		ClientCredentialsInfo credentials = new ClientCredentialsInfo(
				management.getClientIdForOrganization(organization.getUuid()),
				management.newClientSecretForOrganization(organization
						.getUuid()));

		response.setCredentials(credentials);
		return new JSONWithPadding(response, callback);
	}

	public OrganizationInfo getOrganization() {
		return organization;
	}

}
