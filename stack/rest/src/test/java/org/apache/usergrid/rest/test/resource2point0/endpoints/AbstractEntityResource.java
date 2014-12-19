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

package org.apache.usergrid.rest.test.resource2point0.endpoints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import org.apache.usergrid.rest.test.resource.Connection;
import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;

import javax.ws.rs.core.MediaType;
import java.io.IOException;

/**
 * //myorg/myapp/mycollection/myentityid
 */
public abstract class AbstractEntityResource<T extends Entity> extends NamedResource {

    public AbstractEntityResource(String identifier, ClientContext context, UrlResource parent) {
        super(identifier, context, parent);
    }

    protected ObjectMapper mapper = new ObjectMapper();

    public T get() {
        WebResource resource = getResource(true);
        try {
            ApiResponse response = resource.type(MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON)
                    .get(ApiResponse.class);
            return instantiateT(response);
        } catch (UniformInterfaceException e) {

            ApiResponse response = new ApiResponse();
            //copy exceptions and such from response
            try {
                JsonNode node = mapper.readTree(e.getResponse().getEntity(String.class));
                response.setError(node.get("error").textValue());
                response.setErrorDescription(node.get("error").textValue());
            } catch (IOException e1) {
                response.setError("io_error");
                response.setErrorDescription("The API response was empty or not parsable.  Is the API running?");
            }
            return instantiateT(response);
        }

    }

    public T post(final T entity) {
        WebResource resource = getResource(true);
        try {
            ApiResponse response = resource.type(MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON)
                    .post(ApiResponse.class, entity);
            return instantiateT(response);
        } catch (UniformInterfaceException e) {

            ApiResponse response = new ApiResponse();
            //copy exceptions and such from response
            try {
                JsonNode node = mapper.readTree(e.getResponse().getEntity(String.class));
                response.setError(node.get("error").textValue());
                response.setErrorDescription(node.get("error").textValue());
            } catch (IOException e1) {
                response.setError("io_error");
                response.setErrorDescription("The API response was empty or not parsable.  Is the API running?");
            }
            return instantiateT(response);
        }
    }

    public T put(final T entity) {
        WebResource resource = getResource(true);
        return instantiateT(resource.type(MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON)
                .put(ApiResponse.class, entity));
    }

    public void delete() {
        WebResource resource = getResource(true);
        resource.type(MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON)
                .delete(ApiResponse.class);
    }

    public ConnectionsResource connections(String verb, String collection){
        return new ConnectionsResource(verb,collection,context,this);
    }

    public ConnectionsResource connections(String collection){
        return new ConnectionsResource(collection,context,this);
    }

    protected abstract T instantiateT(ApiResponse response);
}
