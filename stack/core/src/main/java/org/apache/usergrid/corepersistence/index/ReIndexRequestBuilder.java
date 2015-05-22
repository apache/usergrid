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


import java.util.UUID;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;

import com.google.common.base.Optional;


/**
 * A builder interface to build our re-index request
 */
public interface ReIndexRequestBuilder {

    /**
     * Set the application id
     */
    ReIndexRequestBuilder withApplicationId( final UUID applicationId );

    /**
     * Set the collection name.  If not set, every collection will be reindexed
     * @param collectionName
     * @return
     */
    ReIndexRequestBuilder withCollection( final String collectionName );

    /**
     * Set our cursor to resume processing
     * @param cursor
     * @return
     */
    ReIndexRequestBuilder withCursor(final String cursor);


    /**
     * Set the timestamp to re-index entities updated >= this timestamp
     * @param timestamp
     * @return
     */
    ReIndexRequestBuilder withStartTimestamp(final Long timestamp);


    /**
     * Get the application scope
     * @return
     */
    Optional<ApplicationScope> getApplicationScope();

    /**
     * Get the collection name
     * @return
     */
    Optional<String> getCollectionName();

    /**
     * Get the cursor
     * @return
     */
    Optional<String> getCursor();

    /**
     * Get the updated since timestamp
     * @return
     */
    Optional<Long> getUpdateTimestamp();
}
