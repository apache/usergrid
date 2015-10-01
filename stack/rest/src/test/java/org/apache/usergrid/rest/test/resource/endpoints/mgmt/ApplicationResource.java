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

package org.apache.usergrid.rest.test.resource.endpoints.mgmt;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.usergrid.rest.test.resource.endpoints.CollectionEndpoint;
import org.apache.usergrid.rest.test.resource.endpoints.NamedResource;
import org.apache.usergrid.rest.test.resource.endpoints.UrlResource;
import org.apache.usergrid.rest.test.resource.model.Application;
import org.apache.usergrid.rest.test.resource.model.*;
import org.apache.usergrid.rest.test.resource.state.ClientContext;
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


    public org.apache.usergrid.rest.test.resource.model.ApiResponse post(Application application) {
        org.apache.usergrid.rest.test.resource.model.ApiResponse apiResponse = getTarget(true)
            .request()
            .accept( MediaType.APPLICATION_JSON )
            .post( javax.ws.rs.client.Entity.json(application),
                org.apache.usergrid.rest.test.resource.model.ApiResponse.class);
        return apiResponse;
    }

    public org.apache.usergrid.rest.test.resource.model.Entity post(
        org.apache.usergrid.rest.test.resource.model.Entity payload) {

        String responseString = getTarget( true )
            .request()
            .accept(MediaType.APPLICATION_JSON)
            .post( javax.ws.rs.client.Entity.json(payload), String.class);

        logger.debug("Response from post: " + responseString);

        org.apache.usergrid.rest.test.resource.model.ApiResponse response;
        try {
            response = mapper.readValue(
                new StringReader(responseString),
                org.apache.usergrid.rest.test.resource.model.ApiResponse.class);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing response", e);
        }

        return new org.apache.usergrid.rest.test.resource.model.Entity(response);
    }


    public org.apache.usergrid.rest.test.resource.model.Entity get() {

        String responseString = getTarget(true)
            .request()
            .accept(MediaType.APPLICATION_JSON)
            .get(String.class);

        logger.debug("Response from post: " + responseString);

        org.apache.usergrid.rest.test.resource.model.ApiResponse response;
        try {
            response = mapper.readValue(
                new StringReader(responseString), org.apache.usergrid.rest.test.resource.model.ApiResponse.class);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing response", e);
        }

        return new org.apache.usergrid.rest.test.resource.model.Entity(response);
    }
}
