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

import org.jclouds.rest.ResourceNotFoundException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.persistence.exceptions.EntityNotFoundException;
import org.usergrid.rest.AbstractContextResource;
import org.usergrid.rest.ApiResponse;
import org.usergrid.rest.security.annotations.RequireOrganizationAccess;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.sun.jersey.api.json.JSONWithPadding;

@Component("org.usergrid.rest.management.organizations.applications.ApplicationsResource")
@Scope("prototype")
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

		ApiResponse response = createApiResponse();
		response.setAction("get organization application");

		BiMap<UUID, String> applications = management
				.getApplicationsForOrganization(organization.getUuid());
		response.setData(applications.inverse());

		return new JSONWithPadding(response, callback);
	}

	@RequireOrganizationAccess
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public JSONWithPadding newApplicationForOrganization(@Context UriInfo ui,
			Map<String, Object> json,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {
		String applicationName = (String) json.get("name");
		return newApplicationForOrganizationFromForm(ui, json, callback, applicationName);
	}

	@RequireOrganizationAccess
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public JSONWithPadding newApplicationForOrganizationFromForm(
			@Context UriInfo ui, Map<String, Object> json,
			@QueryParam("callback") @DefaultValue("callback") String callback,
			@FormParam("name") String applicationName) throws Exception {

    Preconditions.checkArgument(!isEmpty(applicationName),
            "The 'name' parameter is required and cannot be empty: " + applicationName);

		ApiResponse response = createApiResponse();
		response.setAction("new application for organization");

		ApplicationInfo applicationInfo = management.createApplication(
				organization.getUuid(), applicationName);

		LinkedHashMap<String, UUID> applications = new LinkedHashMap<String, UUID>();
		applications.put(applicationInfo.getName(), applicationInfo.getId());
		response.setData(applications);
        response.setResults(management.getApplicationMetadata(applicationInfo.getId()));
		return new JSONWithPadding(response, callback);
	}

	@RequireOrganizationAccess
	@Path("{applicationId: [A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}}")
	public ApplicationResource applicationFromOrganizationByApplicationId(
			@Context UriInfo ui,
			@PathParam("applicationId") String applicationIdStr)
			throws Exception {

		return getSubResource(ApplicationResource.class).init(organization,
				UUID.fromString(applicationIdStr));
	}

	@RequireOrganizationAccess
	@Path("{applicationName}")
	public ApplicationResource applicationFromOrganizationByApplicationName(
			@Context UriInfo ui,
			@PathParam("applicationName") String applicationName)
			throws Exception {

    String appName = applicationName.contains("/") ? applicationName : organization.getName() + "/" + applicationName;

		ApplicationInfo application = management
				.getApplicationInfo(appName);

		if(application == null){
		  throw new EntityNotFoundException(String.format("Application %s does not exist for organization %s", applicationName,organization.getName() ));
		}
		
		return getSubResource(ApplicationResource.class).init(organization,
				application);
	}
}
