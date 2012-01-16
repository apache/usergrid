package org.usergrid.rest.management;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sun.jersey.api.view.Viewable;

@Path("/management.json")
@Component
@Scope("singleton")
@Produces({ MediaType.APPLICATION_JSON, "application/javascript",
		"application/x-javascript", "text/ecmascript",
		"application/ecmascript", "text/jscript" })
public class SwaggerManagementResource {

	public static final Logger logger = LoggerFactory
			.getLogger(SwaggerManagementResource.class);

	public SwaggerManagementResource() {

	}

	@GET
	public Viewable discoverResources(@Context UriInfo ui) throws Exception {

		return new Viewable("management.json", this);
	}

}
