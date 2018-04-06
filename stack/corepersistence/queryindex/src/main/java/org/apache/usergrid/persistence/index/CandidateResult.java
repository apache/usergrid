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
    private Id entityId;
    private final UUID entityVersion;
    private final String docId;
    private final String directEntityName;
    private final UUID directEntityUUID;
    private final String directEntityType;

    public CandidateResult( Id entityId, UUID entityVersion, String docId ) {
        this.entityId = entityId;
        this.entityVersion = entityVersion;
        this.docId = docId;
        this.directEntityName = null;
        this.directEntityUUID = null;
        this.directEntityType = null;
    }

    public CandidateResult( Id entityId, CandidateResult sourceResult ) {
        this.entityId = entityId;
        this.entityVersion = sourceResult.entityVersion;
        this.docId = sourceResult.docId;
        this.directEntityName = sourceResult.directEntityName;
        this.directEntityUUID = sourceResult.directEntityUUID;
        this.directEntityType = sourceResult.directEntityType;
    }

    // direct query by name before resolution
    public CandidateResult( String entityType, String directEntityName ) {
        this.directEntityName = directEntityName;
        this.directEntityUUID = null;
        this.directEntityType = entityType;
        this.entityId = null;
        this.entityVersion = null;
        this.docId = null;
    }

    // direct query by UUID before resolution
    public CandidateResult( String entityType, UUID directEntityUUID ) {
        this.directEntityUUID = directEntityUUID;
        this.directEntityName = null;
        this.directEntityType = entityType;
        this.entityId = null;
        this.entityVersion = null;
        this.docId = null;
    }

    public boolean isDirectQueryName() {
        return directEntityName != null;
    }
    public boolean isDirectQueryUUID() {
        return directEntityUUID != null;
    }
    public boolean isDirectQuery() {
        return isDirectQueryName() || isDirectQueryUUID();
    }

    @Override
    public UUID getVersion() {
        return entityVersion;
    }

    @Override
    public Id getId() {
        return entityId;
    }

    public String getDocId() {
        return docId;
    }

    public String getDirectEntityName() {
        return directEntityName;
    }

    public UUID getDirectEntityUUID() {
        return directEntityUUID;
    }

    public String getDirectEntityType() {
        return directEntityType;
    }

    // use to set id for direct query after resolution
    public void setId(Id entityId) {
        this.entityId = entityId;
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( o == null ) {
            return false;
        }
        if ( !( o instanceof CandidateResult ) ) {
            return false;
        }

        final CandidateResult that = ( CandidateResult ) o;

        if ( entityId == null && that.entityId != null) {
            return false;
        }
        if ( entityId != null && !entityId.equals( that.entityId ) ) {
            return false;
        }
        if ( entityVersion == null && that.entityVersion != null) {
            return false;
        }
        if ( entityVersion != null && !entityVersion.equals( that.entityVersion ) ) {
            return false;
        }
        if ( docId == null && that.docId != null) {
            return false;
        }
        if ( docId != null && !docId.equals( that.docId ) ) {
            return false;
        }
        if ( directEntityUUID != that.directEntityUUID ) {
            return false;
        }
        if ( directEntityName == null && that.directEntityName != null) {
            return false;
        }
        if ( directEntityName != null && !directEntityName.equals( that.directEntityName ) ) {
            return false;
        }
        if ( directEntityType == null && that.directEntityType != null) {
            return false;
        }
        if ( directEntityType != null && !directEntityType.equals( that.directEntityType ) ) {
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
