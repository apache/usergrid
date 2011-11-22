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

import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.usergrid.rest.security.annotations.RequireSystemAccess;

import com.sun.jersey.api.json.JSONWithPadding;

@Path("/system")
@Component
@Scope("singleton")
@Produces({ MediaType.APPLICATION_JSON, "application/javascript",
		"application/x-javascript", "text/ecmascript",
		"application/ecmascript", "text/jscript" })
public class SystemResource extends AbstractContextResource {

	private static final Logger logger = LoggerFactory
			.getLogger(SystemResource.class);

	public SystemResource() {
		logger.info("SystemResource initialized");
	}

	@RequireSystemAccess
	@GET
	@Path("database/setup")
	public JSONWithPadding getSetup(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		ApiResponse response = new ApiResponse(ui);
		response.setAction("cassandra setup");

		logger.info("Setting up Cassandra");

		Map<String, String> properties = emf.getServiceProperties();
		if (properties != null) {
			response.setError("System properties are initialized, database is set up already.");
			return new JSONWithPadding(response, callback);
		}

		try {
			emf.setup();
		} catch (Exception e) {
			logger.error(
					"Unable to complete core database setup, possibly due to it being setup already",
					e);
		}

		try {
			management.setup();
		} catch (Exception e) {
			logger.error(
					"Unable to complete management database setup, possibly due to it being setup already",
					e);
		}

		response.setSuccess();

		return new JSONWithPadding(response, callback);
	}

	@RequireSystemAccess
	@GET
	@Path("hello")
	public JSONWithPadding hello(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {
		logger.info("Saying hello");

		ApiResponse response = new ApiResponse(ui);
		response.setAction("Greetings Professor Falken");
		response.setSuccess();

		return new JSONWithPadding(response, callback);
	}

}
