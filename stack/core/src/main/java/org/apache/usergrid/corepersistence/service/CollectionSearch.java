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
 * Bean for input on searching a collection
 */
public class CollectionSearch {

    private final ApplicationScope applicationScope;
    private final Id collectionOwnerId;
    private final String collectionName;
    private final String entityType;
    private final int limit;
    private final Optional<String> query;
    private final Optional<String> cursor;


    public CollectionSearch( final ApplicationScope applicationScope, final Id collectionOwnerId, final String
        collectionName,
                             final String entityType, final int limit, final Optional<String> query, final Optional<String> cursor ) {
        this.applicationScope = applicationScope;
        this.collectionOwnerId = collectionOwnerId;
        this.collectionName = collectionName;
        this.entityType = entityType;
        this.limit = limit;
        this.query = query;
        this.cursor = cursor;
    }


    public ApplicationScope getApplicationScope() {
        return applicationScope;
    }


    public String getCollectionName() {
        return collectionName;
    }


    public Optional<String> getCursor() {
        return cursor;
    }


    public Optional<String> getQuery() {
        return query;
    }


    public int getLimit() {
        return limit;
    }


    public String getEntityType() {
        return entityType;
    }


    public Id getCollectionOwnerId() {
        return collectionOwnerId;
    }
}
