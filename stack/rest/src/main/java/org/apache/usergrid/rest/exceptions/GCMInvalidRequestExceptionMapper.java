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


import com.google.android.gcm.server.InvalidRequestException;
import org.apache.usergrid.rest.ApiResponse;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.*;
import static org.apache.usergrid.utils.JsonUtils.mapToJsonString;


@Provider
public class GCMInvalidRequestExceptionMapper extends AbstractExceptionMapper<InvalidRequestException> {

    @Override
    public Response toResponse( InvalidRequestException e ) {

        Response.Status status;
        String message;
        // purposely overwrite the 401 case
        if(e.getHttpStatusCode() == 401){

            status = UNAUTHORIZED;
            message = "Invalid GCM API Key or Registration ID(s)";

        }else{
            // let's handle the status codes ourselves and pass-through the details from GCM
            if (e.getHttpStatusCode() >= 400 && e.getHttpStatusCode() <= 499){
                status = BAD_REQUEST;
            }else{
                status = INTERNAL_SERVER_ERROR;
            }
            message = "GCM Status Code: "+ e.getHttpStatusCode()+", Detail: "+e.getMessage();
        }

        // build a proper ApiResponse object
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setError(status.toString());
        apiResponse.setErrorDescription(message);

        // give toResponse() the json string value of the ApiResponse
        return toResponse( status, mapToJsonString(apiResponse) );
    }
}
