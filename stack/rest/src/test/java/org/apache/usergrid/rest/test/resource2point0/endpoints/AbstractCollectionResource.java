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
public abstract class AbstractCollectionResource<T,K> extends NamedResource {
    public AbstractCollectionResource(String name, ClientContext context, UrlResource parent) {
        super(name, context, parent);
    }

    public K getCollectionResource(final String identifier){
        return instantiateK(identifier, context, this);
    }

    /**
     * Get a list of entities
     * @return
     */
    public ApiResponse get( final QueryParameters parameters){
        return get(parameters,true);
    }
    /**
     * Get a list of entities
     * @return
     */
    public ApiResponse get(final QueryParameters parameters, final boolean useToken){
        WebResource resource  = getResource(useToken);
        addParametersToResource(getResource(), parameters);
        return resource.type( MediaType.APPLICATION_JSON_TYPE ).accept(MediaType.APPLICATION_JSON)
                .get(ApiResponse.class);
    }

    /**
     * Post the entity to the users collection
     * @param entity
     * @return
     */
    public T post(final T entity){
        return instantiateT(getResource(true).post(ApiResponse.class, entity));
    }

    /**
     * Put the entity to the users collection
     * @param entity
     * @return
     */
    public T put(final T entity){
        return instantiateT(getResource(true).put(ApiResponse.class, entity));
    }

    protected abstract T instantiateT(ApiResponse response);

    protected abstract K instantiateK(String identifier, ClientContext context, UrlResource parent);

}
