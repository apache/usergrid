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

import java.util.UUID;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;

/**
 * Records one change to an entry: a version, a change type and the changed field.
 * 
 * @author dmjohnson@apigee.com
 */
public class ChangeLogEntry {

    private Id entryId;

    private UUID version;

    /**
     * @return the entryId
     */
    public Id getEntryId() {
        return entryId;
    }

    /**
     * @param entryId the entryId to set
     */
    public void setEntryId( Id entryId ) {
        this.entryId = entryId;
    }

    /**
     * @return the version
     */
    public UUID getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion( UUID version ) {
        this.version = version;
    }

    /**
     * @return the changeType
     */
    public ChangeType getChangeType() {
        return changeType;
    }

    /**
     * @param changeType the changeType to set
     */
    public void setChangeType( ChangeType changeType ) {
        this.changeType = changeType;
    }

    /**
     * @return the changedField
     */
    public Field getChangedField() {
        return changedField;
    }

    /**
     * @param changedField the changedField to set
     */
    public void setChangedField( Field changedField ) {
        this.changedField = changedField;
    }

    public enum ChangeType {
        PROPERTY_WRITE,
        PROPERTY_DELETE
    };

    private ChangeType changeType;

    private Field changedField;
}
