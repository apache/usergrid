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
package org.usergrid.rest;

import static org.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.usergrid.rest.applications.ApplicationResource;
import org.usergrid.rest.exceptions.NoOpException;
import org.usergrid.rest.exceptions.UnauthorizedApiRequestException;

import com.sun.jersey.api.json.JSONWithPadding;

/**
 * 
 * @author ed@anuff.com
 */
@Path("/")
@Component
@Scope("singleton")
@Produces({ MediaType.APPLICATION_JSON, "application/javascript",
		"application/x-javascript", "text/ecmascript",
		"application/ecmascript", "text/jscript" })
public class RootResource extends AbstractContextResource {

	private static final Logger logger = LoggerFactory
			.getLogger(RootResource.class);

	long started = System.currentTimeMillis();

	public RootResource() {
	}

	@GET
	@Path("applications")
	public JSONWithPadding getAllApplications(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws URISyntaxException {

		System.out.println("RootResource.getAllApplications");

		ApiResponse response = new ApiResponse(ui);
		response.setAction("get applications");

		Map<String, UUID> applications = null;
		try {
			applications = emf.getApplications();
			response.setSuccess();
			response.setApplications(applications);
		} catch (Exception e) {
			logger.info("Unable to retrieve applications", e);
			response.setError("Unable to retrieve applications");
		}

		return new JSONWithPadding(response, callback);
	}

	@GET
	public Response getRoot(@Context UriInfo ui) throws URISyntaxException {

		String redirect_root = properties.getProperty("usergrid.redirect_root");
		if (StringUtils.isNotBlank(redirect_root)) {
			ResponseBuilder response = Response.temporaryRedirect(new URI(
					redirect_root));
			return response.build();
		} else {
			ResponseBuilder response = Response.temporaryRedirect(new URI(
					"/status"));
			return response.build();
		}
	}

	@GET
	@Path("status")
	public JSONWithPadding getStatus(
			@QueryParam("callback") @DefaultValue("callback") String callback) {
		ApiResponse response = new ApiResponse();

		ObjectNode node = JsonNodeFactory.instance.objectNode();
		node.put("started", started);
		node.put("uptime", System.currentTimeMillis() - started);
		response.setProperty("status", node);
		return new JSONWithPadding(response, callback);
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

		if (applicationId.equals(MANAGEMENT_APPLICATION_ID)) {
			throw new UnauthorizedApiRequestException();
		}

		return new ApplicationResource(this, applicationId);
	}

	@Path("applications/{applicationId: [A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}}")
	public ApplicationResource getApplicationById2(
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

		UUID applicationId = emf.lookupApplication(applicationName);
		if (applicationId == null) {
			return null;
		}

		if (applicationId.equals(MANAGEMENT_APPLICATION_ID)) {
			throw new UnauthorizedApiRequestException();
		}

		return new ApplicationResource(this, applicationId);
	}

	@Path("applications/{applicationName}")
	public ApplicationResource getApplicationByName2(
			@PathParam("applicationName") String applicationName)
			throws Exception {
		return getApplicationByName(applicationName);
	}

}
