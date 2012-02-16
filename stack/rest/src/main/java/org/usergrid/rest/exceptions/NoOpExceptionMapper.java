package org.usergrid.rest.exceptions;

import static javax.ws.rs.core.Response.Status.OK;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class NoOpExceptionMapper extends AbstractExceptionMapper<NoOpException> {

	@Override
	public Response toResponse(NoOpException e) {
		return toResponse(OK, e.getJsonResponse());
	}

}
