package org.usergrid.rest.applications.users.extensions;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.rest.applications.users.AbstractUserExtensionResource;
import org.usergrid.rest.applications.users.UserResource;

@Ignore
@Produces(MediaType.APPLICATION_JSON)
public class TestResource extends AbstractUserExtensionResource {

	private static Logger log = LoggerFactory.getLogger(TestResource.class);

	public TestResource() {
	}

	@Override
	public TestResource init(UserResource userResource) {
		log.info("TestResource");
		return this;
	}

	@GET
	public String sayHello() {
		return "{\"message\" : \"hello\""
				+ (getUserResource().getUserUuid() != null ? ", \"user\" : \""
						+ getUserResource().getUserUuid() + "\"" : "") + " }";
	}
}
