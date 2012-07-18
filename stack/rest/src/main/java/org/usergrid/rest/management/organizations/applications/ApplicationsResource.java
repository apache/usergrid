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
import static org.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;
import static org.usergrid.services.ServiceParameter.parameters;

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

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.rest.AbstractContextResource;
import org.usergrid.rest.ApiResponse;
import org.usergrid.rest.security.annotations.RequireOrganizationAccess;
import org.usergrid.services.ServiceAction;
import org.usergrid.services.ServiceManager;
import org.usergrid.services.ServiceManagerFactory;
import org.usergrid.services.ServiceRequest;
import org.usergrid.services.ServiceResults;
import org.usergrid.services.applications.ApplicationsService;

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
        // TODO TN this isn't returning the right data. It should just return
        // the same data as get
        ApiResponse response = new ApiResponse(ui);
        response.setAction("new application for organization");
        // TODO change to organizationName/applicationName
        UUID applicationId = management.createApplication(
                organization.getUuid(), applicationName);

        ServiceManager sm = smf.getServiceManager(applicationId);
        ServiceRequest request = sm.newRequest(ServiceAction.GET, parameters());
        ServiceResults results = request.execute();

        response.setData(results.getData());
        //
        // LinkedHashMap<String, UUID> applications = new LinkedHashMap<String,
        // UUID>();
        // applications.put(applicationName, applicationId);
        // response.setData(applications);
        //
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
        // TODO change to organizationName/applicationName
        UUID applicationId = management.createApplication(
                organization.getUuid(), applicationName);

        ServiceManager sm = smf.getServiceManager(applicationId);
        ServiceRequest request = sm.newRequest(ServiceAction.GET, parameters());
        ServiceResults results = request.execute();

        response.setData(results.getData());
        //
        // LinkedHashMap<String, UUID> applications = new LinkedHashMap<String,
        // UUID>();
        // applications.put(applicationName, applicationId);
        // response.setData(applications);

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

        String appName = applicationName.contains("/") ? applicationName
                : organization.getName() + "/" + applicationName;

        ApplicationInfo application = management.getApplicationInfo(appName);

        return getSubResource(ApplicationResource.class).init(organization,
                application);
    }

}
