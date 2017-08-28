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
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;

import java.util.UUID;
import java.util.concurrent.TimeUnit;


/**
 * collection delete service request builder
 */
public class CollectionDeleteRequestBuilderImpl implements CollectionDeleteRequestBuilder {

    private Optional<UUID> withApplicationId = Optional.absent();
    private Optional<String> withCollectionName = Optional.absent();
    private Optional<String> cursor = Optional.absent();
    private Optional<Long> endTimestamp = Optional.absent();
    private Optional<Integer> delayTimer = Optional.absent();
    private Optional<TimeUnit> timeUnitOptional = Optional.absent();


    /***
     *
     * @param applicationId The application id
     * @return
     */
    @Override
    public CollectionDeleteRequestBuilder withApplicationId( final UUID applicationId ) {
        this.withApplicationId = Optional.fromNullable( applicationId );
        return this;
    }


    /**
     * the collection name
     * @param collectionName
     * @return
     */
    @Override
    public CollectionDeleteRequestBuilder withCollection( final String collectionName ) {
        this.withCollectionName = Optional.fromNullable( CpNamingUtils.getEdgeTypeFromCollectionName( collectionName.toLowerCase() ) );
        return this;
    }


    /**
     * The cursor
     * @param cursor
     * @return
     */
    @Override
    public CollectionDeleteRequestBuilder withCursor( final String cursor ) {
        this.cursor = Optional.fromNullable( cursor );
        return this;
    }


    /**
     * Determines whether we should tack on a delay for collection delete and for how long if we do. Also
     * allowed to specify how throttled back it should be.
     * @param delayTimer
     * @param timeUnit
     * @return
     */
    @Override
    public CollectionDeleteRequestBuilder withDelay( final int delayTimer, final TimeUnit timeUnit ){
        this.delayTimer = Optional.fromNullable( delayTimer );
        this.timeUnitOptional = Optional.fromNullable( timeUnit );

        return this;
    }


    /**
     * Set end timestamp in epoch time.  Only entities created before this time will be processed for deletion
     * @param timestamp
     * @return
     */
    @Override
    public CollectionDeleteRequestBuilder withEndTimestamp( final Long timestamp ) {
        this.endTimestamp = Optional.fromNullable( timestamp );
        return this;
    }


    @Override
    public Optional<Integer> getDelayTimer() {
        return delayTimer;
    }

    @Override
    public Optional<TimeUnit> getTimeUnitOptional() {
        return timeUnitOptional;
    }


    @Override
    public Optional<ApplicationScope> getApplicationScope() {

        if ( this.withApplicationId.isPresent() ) {
            return Optional.of( CpNamingUtils.getApplicationScope( withApplicationId.get() ) );
        }

        return Optional.absent();
    }


    @Override
    public Optional<String> getCollectionName() {
        return withCollectionName;
    }


    @Override
    public Optional<String> getCursor() {
        return cursor;
    }


    @Override
    public Optional<Long> getEndTimestamp() {
        return endTimestamp;
    }
}
