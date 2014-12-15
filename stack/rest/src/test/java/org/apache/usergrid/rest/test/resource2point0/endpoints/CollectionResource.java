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
package org.apache.usergrid.rest.test.resource2point0.endpoints;


import java.util.UUID;

import com.sun.jersey.api.client.WebResource;
import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.EntityResponse;
import org.apache.usergrid.rest.test.resource2point0.model.QueryParameters;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;

import com.google.common.base.Optional;

import javax.ws.rs.core.MediaType;


/**
 * Holds POST,PUT,GET,DELETE methods for Collections. Models the rest endpoints for the different ways
 * to get an entity out of UG.
 */
public  class CollectionResource extends NamedResource {


    public CollectionResource(final String name, final ClientContext context, final UrlResource parent) {
        super( name, context, parent );
    }

    public EntityResource getEntityResource(final String identifier){
        return new EntityResource( identifier, context, this );
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
    public ApiResponse post(final Entity entity){
        return getResource(true).post(ApiResponse.class,entity);
    }

    /**
     * Put the entity to the users collection
     * @param entity
     * @return
     */
    public ApiResponse put(final Entity entity){
        return getResource(true).post(ApiResponse.class,entity);
    }





}
