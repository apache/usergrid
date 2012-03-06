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

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.amber.oauth2.common.message.OAuthResponse;

/**
 * <p>
 * Mapper for OAuthProblemException.
 * </p>
 */
@Provider
public class OAuthProblemExceptionMapper implements
		ExceptionMapper<OAuthProblemException> {

	@Override
	public Response toResponse(OAuthProblemException e) {
		OAuthResponse res = null;
		try {
			res = OAuthResponse.errorResponse(SC_BAD_REQUEST).error(e)
					.buildJSONMessage();
		} catch (OAuthSystemException e1) {
		}
		if (res != null) {
			return Response.status(res.getResponseStatus())
					.type(APPLICATION_JSON_TYPE).entity(res.getBody()).build();
		} else {
			return Response.status(SC_BAD_REQUEST).build();
		}
	}

}
