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
package org.apache.usergrid.rest.exceptions;


import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.services.exceptions.PasswordPolicyViolationException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.apache.usergrid.utils.JsonUtils.mapToJsonString;


/** <p> Mapper for OAuthProblemException. </p> */
@Provider
public class PasswordPolicyViolationExceptionMapper extends AbstractExceptionMapper<PasswordPolicyViolationException> {

    @Override
    public Response toResponse( PasswordPolicyViolationException e ) {

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setError( e.getMessage() );

        StringBuilder sb = new StringBuilder();
        for ( String violation : e.getViolations() ) {
            sb.append( violation ).append(" ");
        }
        apiResponse.setErrorDescription( sb.toString() );

        return toResponse( SC_BAD_REQUEST, mapToJsonString(apiResponse) );
    }
}
