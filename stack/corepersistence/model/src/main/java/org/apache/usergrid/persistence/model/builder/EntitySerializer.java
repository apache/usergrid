/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.model.builder;


import org.apache.usergrid.persistence.model.entity.Entity;


/**
 * Responsible for serializing the specific type of entity to and from an object
 *  @author tnine */
public interface EntitySerializer<T> {


    /**
     * Return a full entity or a set of fields?
     *
     * @param object
     * @return
     */
    Entity fromEntity(T object);


    /**
     * Convert the entity to the runtime type we need
     * @param e
     * @return
     */
    T toObject(Entity e);

}
