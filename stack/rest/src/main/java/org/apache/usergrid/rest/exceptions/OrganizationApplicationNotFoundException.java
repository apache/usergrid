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


import javax.ws.rs.core.UriInfo;

import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.ServerEnvironmentProperties;

import static org.apache.usergrid.utils.JsonUtils.mapToJsonString;


/** @author zznate */
public class OrganizationApplicationNotFoundException extends RuntimeException {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private ApiResponse apiResponse;


    public OrganizationApplicationNotFoundException( String orgAppName, UriInfo uriInfo,
                                                     ServerEnvironmentProperties properties ) {
        super( "Could not find application for " + orgAppName + " from URI: " + uriInfo.getPath() );
        apiResponse = new ApiResponse( properties );

        apiResponse.setError( this );
    }


    public ApiResponse getApiResponse() {
        return apiResponse;
    }


    public String getJsonResponse() {
        return mapToJsonString( apiResponse );
    }
}
