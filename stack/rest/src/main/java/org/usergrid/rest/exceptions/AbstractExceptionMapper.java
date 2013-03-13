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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.rest.ApiResponse;

public abstract class AbstractExceptionMapper<E extends java.lang.Throwable>
		implements ExceptionMapper<E> {

	public static final Logger logger = LoggerFactory
			.getLogger(AbstractExceptionMapper.class.getPackage().toString());

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
	
	public Response toResponse(Status status, E e){
	  return toResponse(status.getStatusCode(), e);
	}

	public Response toResponse(int status, E e) {
		logger.error("Error in request (" + status + ")", e);
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
		return toResponse(status.getStatusCode(), jsonResponse);
	}
	
	public Response toResponse(int status, String jsonResponse) {
    logger.error("Error in request (" + status + "):\n" + jsonResponse);
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
