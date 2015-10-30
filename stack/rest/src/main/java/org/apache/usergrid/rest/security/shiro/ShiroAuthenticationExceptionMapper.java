/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.rest.security.shiro;


import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.security.tokens.exceptions.BadTokenException;
import org.apache.usergrid.security.tokens.exceptions.ExpiredTokenException;

import org.apache.shiro.authc.AuthenticationException;

import static org.apache.usergrid.rest.exceptions.AuthErrorInfo.BAD_ACCESS_TOKEN_ERROR;
import static org.apache.usergrid.rest.exceptions.AuthErrorInfo.EXPIRED_ACCESS_TOKEN_ERROR;


@Provider
public class ShiroAuthenticationExceptionMapper implements ExceptionMapper<AuthenticationException> {

    @Override
    public Response toResponse( AuthenticationException e ) {
        if ( e.getCause() != null ) {
            return constructResponse( e.getCause() );
        }
        return constructResponse( e );
    }


    public Response constructResponse( Throwable e ) {
        String type = null;
        String message = e.getMessage();
        ApiResponse response = new ApiResponse();
        if ( e instanceof ExpiredTokenException ) {
            type = EXPIRED_ACCESS_TOKEN_ERROR.getType();
        }
        else if ( e instanceof BadTokenException ) {
            type = BAD_ACCESS_TOKEN_ERROR.getType();
        }
        response.withError( type, message, e );
        return Response.status( Status.UNAUTHORIZED ).type( MediaType.APPLICATION_JSON ).entity( response ).build();
    }
}
