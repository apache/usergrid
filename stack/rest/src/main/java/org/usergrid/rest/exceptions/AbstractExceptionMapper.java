package org.usergrid.rest.exceptions;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.usergrid.rest.utils.JSONPUtils.isJavascript;
import static org.usergrid.rest.utils.JSONPUtils.wrapJSONPResponse;
import static org.usergrid.utils.JsonUtils.mapToJsonString;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import org.usergrid.rest.ApiResponse;

public abstract class AbstractExceptionMapper<E extends java.lang.Throwable>
		implements ExceptionMapper<E> {

	@Context
	HttpHeaders hh;

	@Context
	protected HttpServletRequest httpServletRequest;

	public boolean isJSONP() {
		return isJavascript(hh.getAcceptableMediaTypes());
	}

	@Override
	public Response toResponse(E e) {
		return toResponse(BAD_REQUEST, e);
	}

	public Response toResponse(Status status, E e) {
		ApiResponse response = new ApiResponse();
		AuthErrorInfo authError = AuthErrorInfo.getForException(e);
		if (authError != null) {
			response.setError(authError.getType(), authError.getMessage(), e);
		} else {
			response.setError(e);
		}
		String jsonResponse = mapToJsonString(response);
		return toResponse(status, jsonResponse);
	}

	public Response toResponse(Status status, String jsonResponse) {
		String callback = httpServletRequest.getParameter("callback");
		if (isJSONP() && isNotBlank(callback)) {
			jsonResponse = wrapJSONPResponse(callback, jsonResponse);
			return Response.status(OK).type("application/javascript")
					.entity(jsonResponse).build();
		} else {
			return Response.status(status).type(APPLICATION_JSON_TYPE)
					.entity(jsonResponse).build();

		}
	}

}
