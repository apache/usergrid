/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.collection.serialization.impl;


import java.util.UUID;

import org.apache.usergrid.persistence.model.entity.Id;

/**
 * Combine entity ID and entity version for use as column name for UniqueValues Column Family.
 */
public class EntityVersion {
    private final Id entityId;
    private final UUID entityVersion;

    public EntityVersion(Id id, UUID uuid) {
        this.entityId = id;
        this.entityVersion = uuid;
    }

    public Id getEntityId() {
        return entityId;
    }

    public UUID getEntityVersion() {
        return entityVersion;
    }

    public boolean equals( Object o ) {

        if ( o == null || !(o instanceof EntityVersion) ) {
            return false;
        }

        EntityVersion other = (EntityVersion)o;

        if ( !other.getEntityId().equals( getEntityId() )) {
            return false;
        }

        if ( !other.getEntityVersion().equals( getEntityVersion() )) {
            return false;
        }

        return true;
    }
    
}
