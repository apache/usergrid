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

import com.sun.jersey.api.client.WebResource;
import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.QueryParameters;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;

import javax.ws.rs.core.MediaType;

/**
 * Classy class class.
 */
public class EntityResource extends NamedResource {
    public EntityResource(String name, ClientContext context, UrlResource parent) {
        super(name, context, parent);
    }

    public ApiResponse get(){
        return get(new QueryParameters());
    }

    public ApiResponse get(final QueryParameters parameters){
        WebResource resource  =getResource(true);
        addParametersToResource(resource,parameters);
        return resource.type( MediaType.APPLICATION_JSON_TYPE ).accept(MediaType.APPLICATION_JSON)
                .get(ApiResponse.class);
    }

    public ApiResponse post(final Entity entity){
        WebResource resource  =getResource(true);
        return resource.type( MediaType.APPLICATION_JSON_TYPE ).accept(MediaType.APPLICATION_JSON)
                .post(ApiResponse.class,entity);
    }

    public ApiResponse put(final Entity entity){
        WebResource resource  =getResource(true);
        return resource.type( MediaType.APPLICATION_JSON_TYPE ).accept(MediaType.APPLICATION_JSON)
                .put(ApiResponse.class,entity);
    }
}
