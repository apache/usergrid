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

import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.usergrid.management.ApplicationCreator;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.OrganizationOwnerInfo;
import org.usergrid.management.exceptions.ManagementException;
import org.usergrid.rest.AbstractContextResource;
import org.usergrid.rest.ApiResponse;

import com.sun.jersey.api.json.JSONWithPadding;

@Component("org.usergrid.rest.management.organizations.OrganizationsResource")
@Scope("prototype")
@Produces({ MediaType.APPLICATION_JSON, "application/javascript",
        "application/x-javascript", "text/ecmascript",
        "application/ecmascript", "text/jscript" })
public class OrganizationsResource extends AbstractContextResource {

    private static final Logger logger = LoggerFactory
            .getLogger(OrganizationsResource.class);

    @Autowired
    private ApplicationCreator applicationCreator;

    public OrganizationsResource() {
    }

    @Path("{organizationId: [A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}}")
    public OrganizationResource getOrganizationById(@Context UriInfo ui,
            @PathParam("organizationId") String organizationIdStr)
            throws Exception {
        OrganizationInfo organization = management.getOrganizationByUuid(UUID
                .fromString(organizationIdStr));
        if (organization == null) {
            throw new ManagementException(
                    "Could not find organization for ID: " + organizationIdStr);
        }
        return getSubResource(OrganizationResource.class).init(organization);
    }

    @Path("{organizationName}")
    public OrganizationResource getOrganizationByName(@Context UriInfo ui,
            @PathParam("organizationName") String organizationName)
            throws Exception {
        OrganizationInfo organization = management
                .getOrganizationByName(organizationName);
        if (organization == null) {
            throw new ManagementException(
                    "Could not find organization for name: " + organizationName);
        }
        return getSubResource(OrganizationResource.class).init(organization);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public JSONWithPadding newOrganization(@Context UriInfo ui,
            Map<String, Object> json,
            @QueryParam("callback") @DefaultValue("") String callback)
            throws Exception {
        ApiResponse response = new ApiResponse(ui);
        response.setAction("new organization");

        String organizationName = (String) json.get("organization");
        String username = (String) json.get("username");
        String name = (String) json.get("name");
        String email = (String) json.get("email");
        String password = (String) json.get("password");

        return newOrganization(ui, organizationName, username, name, email,
                password, callback);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public JSONWithPadding newOrganizationFromForm(@Context UriInfo ui,
            @FormParam("organization") String organizationNameForm,
            @QueryParam("organization") String organizationNameQuery,
            @FormParam("username") String usernameForm,
            @QueryParam("username") String usernameQuery,
            @FormParam("name") String nameForm,
            @QueryParam("name") String nameQuery,
            @FormParam("email") String emailForm,
            @QueryParam("email") String emailQuery,
            @FormParam("password") String passwordForm,
            @QueryParam("password") String passwordQuery,
            @QueryParam("callback") @DefaultValue("") String callback)
            throws Exception {

        String organizationName = organizationNameForm != null ? organizationNameForm
                : organizationNameQuery;
        String username = usernameForm != null ? usernameForm : usernameQuery;
        String name = nameForm != null ? nameForm : nameQuery;
        String email = emailForm != null ? emailForm : emailQuery;
        String password = passwordForm != null ? passwordForm : passwordQuery;

        return newOrganization(ui, organizationName, username, name, email,
                password, callback);

    }

    /**
     * Create a new organization
     * 
     * @param ui
     * @param organizationName
     * @param username
     * @param name
     * @param email
     * @param password
     * @param callback
     * @return
     * @throws Exception
     */
    private JSONWithPadding newOrganization(@Context UriInfo ui,
            String organizationName, String username, String name,
            String email, String password, String callback) throws Exception {
        Preconditions.checkArgument(StringUtils.isNotBlank(organizationName),
                "The organization parameter was missing");

        logger.info("New organization: " + organizationName);

        ApiResponse response = new ApiResponse(ui);
        response.setAction("new organization");

        OrganizationOwnerInfo organizationOwner = management
                .createOwnerAndOrganization(organizationName, username, name,
                        email, password, false, false);

        if (organizationOwner == null) {
            return null;
        }

        applicationCreator.createSampleFor(organizationOwner.getOrganization());

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
