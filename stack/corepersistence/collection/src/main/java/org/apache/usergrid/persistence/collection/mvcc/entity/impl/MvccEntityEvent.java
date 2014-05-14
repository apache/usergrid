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

package org.apache.usergrid.persistence.collection.mvcc.entity.impl;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.core.scope.OrganizationScope;

import java.io.Serializable;
import java.util.UUID;

/**
 * Entity Event for queues
 */
public class MvccEntityEvent<T> implements Serializable {

    private final CollectionScope collectionScope;
    private final T data;
    private final UUID version;


    public MvccEntityEvent(final CollectionScope collectionScope, final UUID version, final T data ) {
        this.collectionScope = collectionScope;
        this.data = data;
        this.version = version;
    }

    public CollectionScope getCollectionScope() {
        return collectionScope;
    }

    public UUID getVersion() {
        return version;
    }

    public T getData() {
        return data;
    }
}
