package org.usergrid.rest.organizations;

import static org.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;

import java.util.UUID;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.NotImplementedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.rest.AbstractContextResource;
import org.usergrid.rest.applications.ApplicationResource;
import org.usergrid.rest.exceptions.NoOpException;
import org.usergrid.rest.exceptions.OrganizationApplicationNotFoundException;
import org.usergrid.rest.security.annotations.RequireOrganizationAccess;
import org.usergrid.rest.utils.PathingUtils;
import org.usergrid.security.shiro.utils.SubjectUtils;

import com.google.common.collect.BiMap;
import com.sun.jersey.api.json.JSONWithPadding;

@Component("org.usergrid.rest.organizations.OrganizationResource")
@Scope("prototype")
@Produces({ MediaType.APPLICATION_JSON, "application/javascript",
        "application/x-javascript", "text/ecmascript",
        "application/ecmascript", "text/jscript" })
public class OrganizationResource extends AbstractContextResource {

    String organizationName;

    public OrganizationResource() {

    }

    public OrganizationResource init(String organizationName) {
        this.organizationName = organizationName;
        return this;
    }

    private ApplicationResource appResourceFor(UUID applicationId)
            throws Exception {
        if (applicationId.equals(MANAGEMENT_APPLICATION_ID)
                && !SubjectUtils.isServiceAdmin()) {
            throw new UnauthorizedException();
        }

        return getSubResource(ApplicationResource.class).init(applicationId);
    }

    @Path("{applicationId: [A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}}")
    public ApplicationResource getApplicationById(
            @PathParam("applicationId") String applicationIdStr)
            throws Exception {

        if ("options".equalsIgnoreCase(request.getMethod())) {
            throw new NoOpException();
        }

        UUID applicationId = UUID.fromString(applicationIdStr);
        if (applicationId == null) {
            return null;
        }

        OrganizationInfo org_info = management
                .getOrganizationByName(organizationName);
        UUID organizationId = null;
        if (org_info != null) {
            organizationId = org_info.getUuid();
        }
        if (applicationId == null || organizationId == null) {
            return null;
        }
        BiMap<UUID, String> apps = management
                .getApplicationsForOrganization(organizationId);
        if (apps.get(applicationId) == null) {
            return null;
        }

        return appResourceFor(applicationId);
    }

    @Path("applications/{applicationId: [A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}}")
    public ApplicationResource getApplicationById2(
            @PathParam("applicationId") String applicationId) throws Exception {
        return getApplicationById(applicationId);
    }

    @Path("apps/{applicationId: [A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}}")
    public ApplicationResource getApplicationById3(
            @PathParam("applicationId") String applicationId) throws Exception {
        return getApplicationById(applicationId);
    }

    @Path("{applicationName}")
    public ApplicationResource getApplicationByName(
            @PathParam("applicationName") String applicationName)
            throws Exception {

        if ("options".equalsIgnoreCase(request.getMethod())) {
            throw new NoOpException();
        }

        String orgAppName = PathingUtils.assembleAppName(organizationName,
                applicationName);
        UUID applicationId = emf.lookupApplication(orgAppName);
        if (applicationId == null) {
            throw new OrganizationApplicationNotFoundException(orgAppName,
                    uriInfo);
        }

        return appResourceFor(applicationId);
    }

    @Path("applications/{applicationName}")
    public ApplicationResource getApplicationByName2(
            @PathParam("applicationName") String applicationName)
            throws Exception {
        return getApplicationByName(applicationName);
    }

    @Path("apps/{applicationName}")
    public ApplicationResource getApplicationByName3(
            @PathParam("applicationName") String applicationName)
            throws Exception {
        return getApplicationByName(applicationName);
    }

    @Path("a/{applicationName}")
    public ApplicationResource getApplicationByName4(
            @PathParam("applicationName") String applicationName)
            throws Exception {
        return getApplicationByName(applicationName);
    }
    

    @DELETE
    @RequireOrganizationAccess
    public JSONWithPadding executeDelete(@Context UriInfo ui,
            @QueryParam("callback") @DefaultValue("callback") String callback) throws Exception {


        throw new NotImplementedException("Organization delete is not allowed yet");
    }

}
