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

package org.apache.usergrid.persistence.index;


import java.util.UUID;

import org.apache.usergrid.persistence.core.entity.EntityVersion;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 * An instance of a candidate result
 */
public class CandidateResult implements EntityVersion {
    private final Id entityId;
    private final UUID entityVersion;

    public CandidateResult( Id entityId, UUID entityVersion ) {
        this.entityId = entityId;
        this.entityVersion = entityVersion;
    }

    @Override
    public UUID getVersion() {
        return entityVersion;
    }

    @Override
    public Id getId() {
        return entityId;
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof CandidateResult ) ) {
            return false;
        }

        final CandidateResult that = ( CandidateResult ) o;

        if ( !entityId.equals( that.entityId ) ) {
            return false;
        }
        if ( !entityVersion.equals( that.entityVersion ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = entityId.hashCode();
        result = 31 * result + entityVersion.hashCode();
        return result;
    }
}
