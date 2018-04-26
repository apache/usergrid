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


import com.google.common.base.Optional;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;

import java.util.UUID;
import java.util.concurrent.TimeUnit;


/**
 * A builder interface to build our collection delete request
 */
public interface CollectionDeleteRequestBuilder {

    /**
     * Set the application id
     */
    CollectionDeleteRequestBuilder withApplicationId(final UUID applicationId);

    /**
     * Set the collection name.
     * @param collectionName
     * @return
     */
    CollectionDeleteRequestBuilder withCollection(final String collectionName);

    /**
     * Set our cursor to resume processing
     * @param cursor
     * @return
     */
    CollectionDeleteRequestBuilder withCursor(final String cursor);


    CollectionDeleteRequestBuilder withDelay(int delayTimer, TimeUnit timeUnit);

    /**
     * Set the timestamp to delete entities updated <= this timestamp
     * @param timestamp
     * @return
     */
    CollectionDeleteRequestBuilder withEndTimestamp(final Long timestamp);


    Optional<Integer> getDelayTimer();

    Optional<TimeUnit> getTimeUnitOptional();

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
     * Get the latest timestamp to delete
     * @return
     */
    Optional<Long> getEndTimestamp();
}
