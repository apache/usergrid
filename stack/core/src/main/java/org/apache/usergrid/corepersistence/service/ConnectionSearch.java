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

package org.apache.usergrid.corepersistence.service;


import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;


/**
 * Bean for input on searching a connection
 */
public class ConnectionSearch {

    private final ApplicationScope applicationScope;
    private final Id sourceNodeId;
    private final Optional<String> entityType;
    private final String connectionName;
    private final int limit;
    private final Optional<String> query;
    private final Optional<String> cursor;


    public ConnectionSearch( final ApplicationScope applicationScope, final Id sourceNodeId, final Optional<String> entityType,
                             final String connectionName, final int limit, final Optional<String> query, final
                             Optional<String> cursor ) {
        this.applicationScope = applicationScope;
        this.sourceNodeId = sourceNodeId;
        this.entityType = entityType;
        this.connectionName = connectionName;
        this.limit = limit;
        this.query = query;
        this.cursor = cursor;
    }


    public ApplicationScope getApplicationScope() {
        return applicationScope;
    }


    public String getConnectionName() {
        return connectionName;
    }


    public Optional<String> getCursor() {
        return cursor;
    }


    public int getLimit() {
        return limit;
    }


    public Optional<String> getQuery() {
        return query;
    }


    public Id getSourceNodeId() {
        return sourceNodeId;
    }


    public Optional<String> getEntityType() {
        return entityType;
    }
}
