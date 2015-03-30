/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */

package org.apache.usergrid.rest.test.resource2point0.endpoints.mgmt;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.usergrid.rest.test.resource2point0.endpoints.CollectionEndpoint;
import org.apache.usergrid.rest.test.resource2point0.endpoints.NamedResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.UrlResource;
import org.apache.usergrid.rest.test.resource2point0.model.Application;
import org.apache.usergrid.rest.test.resource2point0.model.*;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;


/**
 * Classy class class.
 */
public class ApplicationResource extends NamedResource {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationResource.class);

    ObjectMapper mapper = new ObjectMapper();

    public ApplicationResource(ClientContext context, UrlResource parent) {
        super("applications", context, parent);
    }

    public ApplicationResource( final String name, final ClientContext context, final UrlResource parent ) {
        super( name, context, parent );
    }

    public ApplicationResource addToPath( String pathPart ) {
        return new ApplicationResource( pathPart, context, this );
    }


    public ApiResponse post(Application application) {
        ApiResponse apiResponse =getResource(true).type(MediaType.APPLICATION_JSON_TYPE)
            .accept(MediaType.APPLICATION_JSON).post(ApiResponse.class,application);
        return apiResponse;
    }

    public Entity post(Entity payload) {

        String responseString = getResource(true)
            .type( MediaType.APPLICATION_JSON_TYPE )
            .accept(MediaType.APPLICATION_JSON)
            .post(String.class, payload);

        logger.debug("Response from post: " + responseString);

        ApiResponse response;
        try {
            response = mapper.readValue(new StringReader(responseString), ApiResponse.class);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing response", e);
        }

        return new Entity(response);
    }


    public Entity get() {

        String responseString = getResource(true)
            .type( MediaType.APPLICATION_JSON_TYPE )
            .accept(MediaType.APPLICATION_JSON)
            .get(String.class);

        logger.debug("Response from post: " + responseString);

        ApiResponse response;
        try {
            response = mapper.readValue(new StringReader(responseString), ApiResponse.class);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing response", e);
        }

        return new Entity(response);
    }
}
