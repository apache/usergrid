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
package org.apache.usergrid.persistence.collection;


import org.apache.usergrid.persistence.core.entity.EntityVersion;
import org.apache.usergrid.persistence.model.entity.Entity;

import com.google.common.base.Optional;


/**
 * An entity with internal information for versioning
 */
public interface MvccEntity extends EntityVersion{

    /**
     * The possible State of the mvccEntity
     */
    public enum Status {

        /**
         * The entity being written represents a complete entity
         */
        COMPLETE,

        /**
         * The entity being written represents a partial entity.
         *
         * This will be removed after v3 serialization deployment
         */
        @Deprecated()
        PARTIAL,

        /**
         * This entity has been marked as deleted
         */
        DELETED
        ;
    }


    /**
     * Get the entity for this context.
     *
     * @return This will return absent if no data is present.  Otherwise the entity will be contained within the
     *         optional
     */
    Optional<Entity> getEntity();

    /**
     * Get the status of the entity
     */
    Status getStatus();

    /**
     * entity byte size
     * @return
     */
    long getSize();

    void setSize(long size);
}
