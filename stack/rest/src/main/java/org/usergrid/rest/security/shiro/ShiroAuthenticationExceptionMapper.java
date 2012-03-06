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
package org.usergrid.rest.security.shiro;

import static org.usergrid.rest.exceptions.AuthErrorInfo.BAD_ACCESS_TOKEN_ERROR;
import static org.usergrid.rest.exceptions.AuthErrorInfo.EXPIRED_ACCESS_TOKEN_ERROR;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.shiro.authc.AuthenticationException;
import org.usergrid.management.exceptions.BadAccessTokenException;
import org.usergrid.management.exceptions.ExpiredTokenException;
import org.usergrid.rest.ApiResponse;

@Provider
public class ShiroAuthenticationExceptionMapper implements
		ExceptionMapper<AuthenticationException> {

	@Override
	public Response toResponse(AuthenticationException e) {
		if (e.getCause() != null) {
			return constructResponse(e.getCause());
		}
		return constructResponse(e);
	}

	public Response constructResponse(Throwable e) {
		String type = null;
		String message = e.getMessage();
		ApiResponse response = new ApiResponse();
		if (e instanceof ExpiredTokenException) {
			type = EXPIRED_ACCESS_TOKEN_ERROR.getType();
		} else if (e instanceof BadAccessTokenException) {
			type = BAD_ACCESS_TOKEN_ERROR.getType();
		}
		response.withError(type, message, e);
		return Response.status(Status.UNAUTHORIZED).type("application/json")
				.entity(response).build();
	}

}
