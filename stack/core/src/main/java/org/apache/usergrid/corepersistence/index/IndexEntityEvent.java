/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence.index;


import java.io.Serializable;
import java.util.UUID;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 * The immutable serializable event that represents and index operations
 */
public class IndexEntityEvent implements Serializable {

    public ApplicationScope applicationScope;
    public Id entityId;
    public UUID entityVersion;


    public IndexEntityEvent( final ApplicationScope applicationScope, final Id entityId, final UUID entityVersion ) {
        this.applicationScope = applicationScope;
        this.entityId = entityId;
        this.entityVersion = entityVersion;
    }


    public ApplicationScope getApplicationScope() {
        return applicationScope;
    }


    public void setApplicationScope( final ApplicationScope applicationScope ) {
        this.applicationScope = applicationScope;
    }


    public Id getEntityId() {
        return entityId;
    }


    public void setEntityId( final Id entityId ) {
        this.entityId = entityId;
    }


    public UUID getEntityVersion() {
        return entityVersion;
    }


    public void setEntityVersion( final UUID entityVersion ) {
        this.entityVersion = entityVersion;
    }
}
