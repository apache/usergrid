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

import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.EntityResponse;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;

import com.google.common.base.Optional;


/**
 * Holds POST,PUT,GET,DELETE methods for Collections. Models the rest endpoints for the different ways
 * to get an entity out of UG.
 */
public abstract class Collection extends NamedResource {


    public Collection( final String name, final ClientContext context,  final UrlResource parent ) {
        super( name, context, parent );
    }


    /**
     * Get a list of entities
     * @return
     */
    public ApiResponse get(final Optional<String> cursor){
      return getResource().get( ApiResponse.class );
    }


    /**
     * Get the response as an entity response
     * @return
     */
    public EntityResponse getEntityResponse(){
        return EntityResponse.fromCollection( this );
    }


    /**
     * Post the entity to the users collection
     * @param user
     * @return
     */
    public Entity post(final Entity user){
        return null;
    }


    /**
     * Get the entity by uuid
     * @param uuid
     * @return
     */
    public Entity get(final UUID uuid){
        return get(uuid.toString());
    }


    /**
     * Get the entity by name
     * @param name
     * @return
     */
    public Entity get(final String name){
        return null;
    }


    /**
     * Updte the entity
     * @param toUpdate
     * @return
     */
    public Entity put(final Entity toUpdate){
        return null;
    }


    /**
     * Delete the entity
     * @param uuid
     * @return
     */
    public Entity delete(final UUID uuid){
        return delete(uuid.toString());
    }


    /**
     * Delete the entity by name
     * @param name
     * @return
     */
    public Entity delete(final String name){
        return null;
    }
}
