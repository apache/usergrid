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
package org.apache.usergrid.persistence.index.impl.external;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.index.IndexEdge;
import org.apache.usergrid.persistence.index.SearchEdge;
import org.apache.usergrid.persistence.index.impl.IndexEdgeImpl;
import org.apache.usergrid.persistence.index.impl.IndexingUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.EntityMap;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DocIdGenerator {

    private UUID managementAppUUID;
    private UUID managementAppVersion;

    private Id managementAppId;

    protected static final String APPLICATION = "application";
    protected static final String ROLE = "role";
    protected static final String APPLICATION_INFO = "application_info";

    DocIdGenerator() {
        this.managementAppUUID = UUIDGenerator.newTimeUUID();
        this.managementAppVersion = this.managementAppUUID;
        this.managementAppId = new SimpleId(managementAppUUID, APPLICATION);
    }

    DocIdGenerator(UUID managementAppUUID) {
        this.managementAppUUID = managementAppUUID;
        this.managementAppVersion = managementAppUUID;
        this.managementAppId = new SimpleId(managementAppUUID, APPLICATION);
    }

    DocIdGenerator(UUID managementAppUUID, UUID managementAppVersion) {
        this.managementAppUUID = managementAppUUID;
        this.managementAppVersion = managementAppVersion;
        this.managementAppId = new SimpleId(managementAppUUID, APPLICATION);
    }

    public String getRoleDocIdForApp(UUID applicationUUID, long timestamp) {
        Id appId = new SimpleId(applicationUUID, APPLICATION);
        ApplicationScope applicationScope = new ApplicationScopeImpl(appId);
        Id entityId = new SimpleId(UUIDGenerator.newTimeUUID(), ROLE);
        EntityMap roleEntityMap = new EntityMap(entityId, entityId.getUuid());
        Entity roleEntity = Entity.fromMap(roleEntityMap);
        IndexEdge indexEdge = new IndexEdgeImpl(entityId, "zzzcollzzz|roles", SearchEdge.NodeType.SOURCE, timestamp);
        return IndexingUtils.createIndexDocId(applicationScope, roleEntity, indexEdge);
    }

    public List<String> getAppEdgeDocIds(UUID applicationUUD, UUID applicationVersion, long timestamp) {
        ApplicationScope managementApplicationScope = new ApplicationScopeImpl(managementAppId);
        Id appEntityId = new SimpleId(applicationUUD, APPLICATION_INFO);
        EntityMap entityMap = new EntityMap(appEntityId, applicationVersion);
        Entity appEntity = Entity.fromMap(entityMap);
        List<String> ids = new ArrayList<>(2);
        for (String name : new String[]{"zzzcollzzz|application_infos", "zzzconnzzz|owns"}) {
            IndexEdge indexEdge = new IndexEdgeImpl(appEntityId, name, SearchEdge.NodeType.SOURCE, timestamp);
            String docId = IndexingUtils.createIndexDocId(managementApplicationScope, appEntity, indexEdge);
            ids.add(docId);
        }
        return ids;
    }

    public Map.Entry<String, UUID> getCollectionEntityIdForApp(UUID applicationUUID, String collectionType, long timestamp) {
        Id collectionEntityId = new SimpleId(UUIDGenerator.newTimeUUID(), collectionType);
        EntityMap collectionEntityMap = new EntityMap(collectionEntityId, collectionEntityId.getUuid());
        Entity collectionEntity = Entity.fromMap(collectionEntityMap);
        Id applicationId = new SimpleId(applicationUUID, APPLICATION);
        ApplicationScope applicationScope = new ApplicationScopeImpl(applicationId);
        IndexEdge indexEdge = new IndexEdgeImpl(collectionEntityId, "zzzcollzzz|"+collectionType+"s", SearchEdge.NodeType.SOURCE, timestamp);
        return new AbstractMap.SimpleImmutableEntry<String, UUID>(IndexingUtils.createIndexDocId(applicationScope, collectionEntity, indexEdge), collectionEntityId.getUuid());
    }
}
