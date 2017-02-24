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


import org.apache.usergrid.persistence.index.exceptions.QueryAnalyzerEnforcementException;
import org.apache.usergrid.rest.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static org.apache.usergrid.utils.JsonUtils.mapToJsonString;


@Provider
public class QueryAnalyzerEnforcementExceptionMapper extends AbstractExceptionMapper<QueryAnalyzerEnforcementException> {

    private static final Logger logger = LoggerFactory.getLogger( QueryAnalyzerEnforcementExceptionMapper.class );

    @Override
    public Response toResponse( QueryAnalyzerEnforcementException e ) {

        // build a proper ApiResponse object
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setError("query_analyzer_violations_enforced");
        apiResponse.setErrorDescription(e.getErrorMessage());

        logger.warn(e.getErrorMessage());

        // give toResponse() the json string value of the ApiResponse
        // skip logging because we use a 5XX but it's not technically an error for the logs
        return toResponse( SERVICE_UNAVAILABLE, mapToJsonString(apiResponse), true );
    }
}
