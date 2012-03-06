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
import org.apache.shiro.authz.UnauthorizedException;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.usergrid.rest.applications.ApplicationResource;
import org.usergrid.rest.exceptions.NoOpException;

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
	@Path("apps")
	public JSONWithPadding getAllApplications2(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws URISyntaxException {
		return getAllApplications(ui, callback);
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
			throw new UnauthorizedException();
		}

		return getSubResource(ApplicationResource.class).init(applicationId);
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

		UUID applicationId = emf.lookupApplication(applicationName);
		if (applicationId == null) {
			return null;
		}

		if (applicationId.equals(MANAGEMENT_APPLICATION_ID)) {
			throw new UnauthorizedException();
		}

		return getSubResource(ApplicationResource.class).init(applicationId);
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

}
