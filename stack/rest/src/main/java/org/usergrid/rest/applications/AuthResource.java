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
package org.usergrid.rest.applications;

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
import org.usergrid.rest.AbstractContextResource;
import org.usergrid.rest.ApiResponse;
import org.usergrid.services.ServiceManager;

import com.sun.jersey.api.json.JSONWithPadding;

@Component
@Scope("prototype")
@Produces({ MediaType.APPLICATION_JSON, "application/javascript",
		"application/x-javascript", "text/ecmascript",
		"application/ecmascript", "text/jscript" })
public class AuthResource extends AbstractContextResource {

	private static final Logger logger = LoggerFactory
			.getLogger(AuthResource.class);

	ServiceManager services = null;

	public AuthResource() {
	}

	@Override
	public void setParent(AbstractContextResource parent) {
		super.setParent(parent);
		if (parent instanceof ServiceResource) {
			services = ((ServiceResource) parent).services;
		}
	}

	@GET
	@Path("fb")
	public JSONWithPadding authFB(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback) {

		logger.info("AuthResource.authFB");

		ApiResponse response = new ApiResponse(ui);
		response.setAction("setup");
		response.setSuccess();

		return new JSONWithPadding(response, callback);
	}

}
