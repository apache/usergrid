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
package org.apache.usergrid.persistence.collection.mvcc.changelog;

import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;

/**
 * Records one change to an entry field: entry ID, version, change type and the changed field.
 */
public class ChangeLogEntry {

    private final Id entityId;

    private final Set<UUID> versions = new TreeSet<UUID>();
    
    public enum ChangeType {
        PROPERTY_WRITE,
        PROPERTY_DELETE
    };

    private final ChangeType changeType;

    private final Field changedField;

    public ChangeLogEntry(Id entryId, UUID version, ChangeType changeType, Field changedField) {
        this.entityId = entryId;
        if (version != null) {
            this.versions.add(version);
        }
        this.changeType = changeType;
        this.changedField = changedField;
    }

    /**
     * @return the entityId
     */
    public Id getEntryId() {
        return entityId;
    }

    /**
     * @return the version
     */
    public Set<UUID> getVersions() {
        return versions;
    }

    /**
     * @param version the version to set
     */
    public void addVersion( UUID version ) {
        this.versions.add(version);
    }

    /**
     * @return the changeType
     */
    public ChangeType getChangeType() {
        return changeType;
    }

    /**
     * @return the changedField
     */
    public Field getChangedField() {
        return changedField;
    }

    public String toString() {
        return    "Type = " + changeType.toString()
                + ", Property = " + changedField.getName() 
                + ", Value = " + changedField.getValue()
                + ", Versions = " + versions.toString();
    }
}
