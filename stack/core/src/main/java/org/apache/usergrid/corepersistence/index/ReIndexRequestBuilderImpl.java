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

import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;

import com.google.common.base.Optional;


/**
 * Index service request builder
 */
public class ReIndexRequestBuilderImpl implements ReIndexRequestBuilder {

    private Optional<UUID> withApplicationId = Optional.absent();
    private Optional<String> withCollectionName = Optional.absent();
    private Optional<String> cursor = Optional.absent();
    private Optional<Long> updateTimestamp = Optional.absent();


    /***
     *
     * @param applicationId The application id
     * @return
     */
    @Override
    public ReIndexRequestBuilder withApplicationId( final UUID applicationId ) {
        this.withApplicationId = Optional.fromNullable( applicationId );
        return this;
    }


    /**
     * the colleciton name
     * @param collectionName
     * @return
     */
    @Override
    public ReIndexRequestBuilder withCollection( final String collectionName ) {
        if(collectionName == null){
            this.withCollectionName = Optional.absent();
        }
        else {
            this.withCollectionName = Optional.fromNullable( CpNamingUtils.getEdgeTypeFromCollectionName( collectionName.toLowerCase() ) );
        }
        return this;
    }


    /**
     * The cursor
     * @param cursor
     * @return
     */
    @Override
    public ReIndexRequestBuilder withCursor( final String cursor ) {
        this.cursor = Optional.fromNullable( cursor );
        return this;
    }


    /**
     * Set start timestamp in epoch time.  Only entities updated since this time will be processed for indexing
     * @param timestamp
     * @return
     */
    @Override
    public ReIndexRequestBuilder withStartTimestamp( final Long timestamp ) {
        this.updateTimestamp = Optional.fromNullable( timestamp );
        return this;
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
    public Optional<Long> getUpdateTimestamp() {
        return updateTimestamp;
    }
}
