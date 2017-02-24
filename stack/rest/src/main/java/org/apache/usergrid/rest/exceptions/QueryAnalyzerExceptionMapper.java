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


import org.apache.usergrid.persistence.index.QueryAnalyzer;
import org.apache.usergrid.persistence.index.exceptions.QueryAnalyzerException;
import org.apache.usergrid.rest.ApiResponse;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import java.util.Collections;
import java.util.HashMap;

import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.usergrid.utils.JsonUtils.mapToFormattedJsonString;


@Provider
public class QueryAnalyzerExceptionMapper extends AbstractExceptionMapper<QueryAnalyzerException> {

    // This mapper is used to short-circuit the query process on any collection query.  Therefore, it does its best
    // job to build a normal ApiResponse format with 200 OK status.

    @Override
    public Response toResponse( QueryAnalyzerException e ) {


        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.add("ql", e.getOriginalQuery());
        params.add("analyzeOnly", "true");

        // build a proper ApiResponse object
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setParams(params);
        apiResponse.setSuccess();

        // remove large_index warnings because indexes are shared buckets and not specific for an app
        for( int i=0; i < e.getViolations().size(); i++) {
            if (e.getViolations().get(i).get(QueryAnalyzer.k_violation) == QueryAnalyzer.v_large_index) {
                e.getViolations().remove(i);
            }
        }

        apiResponse.setMetadata(new HashMap<String,Object>(){{put("queryWarnings", e.getViolations());}});
        apiResponse.setEntities(Collections.emptyList());
        apiResponse.setAction("query analysis only");

        // give toResponse() the json string value of the ApiResponse
        return toResponse( OK, mapToFormattedJsonString(apiResponse) );
    }
}
