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


public class IndexServiceRequestBuilderImpl implements IndexServiceRequestBuilder {


    /**
     *
     final Observable<ApplicationScope>  applicationScopes = appId.isPresent()? Observable.just( getApplicationScope(appId.get()) ) : allApplicationsObservable.getData();

     final String newCursor = StringUtils.sanitizeUUID( UUIDGenerator.newTimeUUID() );

     //create an observable that loads each entity and indexes it, start it running with publish
     final ConnectableObservable<EdgeScope> runningReIndex =
         allEntityIdsObservable.getEdgesToEntities( applicationScopes, collection, startTimestamp )

             //for each edge, create our scope and index on it
             .doOnNext( edge -> indexService.index( new EntityIdScope( edge.getApplicationScope(), edge.getEdge().getTargetNode() ) ) ).publish();



     //start our sampler and state persistence
     //take a sample every sample interval to allow us to resume state with minimal loss
     runningReIndex.sample( indexProcessorFig.getReIndexSampleInterval(), TimeUnit.MILLISECONDS,
         rxTaskScheduler.getAsyncIOScheduler() )
         .doOnNext( edge -> {

             final String serializedState = SerializableMapper.asString( edge );

             mapManager.putString( newCursor, serializedState, INDEX_TTL );
         } ).subscribe();


     */

    private Optional<UUID> withApplicationId;
    private Optional<String> withCollectionName;
    private Optional<String> cursor;
    private Optional<Long> updateTimestamp;


    /***
     *
     * @param applicationId
     * @return
     */
    @Override
    public IndexServiceRequestBuilder withApplicationId( final UUID applicationId ) {
        this.withApplicationId = Optional.fromNullable(applicationId);
        return this;
    }


    @Override
    public IndexServiceRequestBuilder withCollection( final String collectionName ) {
        this.withCollectionName = Optional.fromNullable( collectionName );
        return this;
    }


    @Override
    public IndexServiceRequestBuilder withCursor( final String cursor ) {
        this.cursor = Optional.fromNullable( cursor );
        return this;
    }


    @Override
    public IndexServiceRequestBuilder withStartTimestamp( final Long timestamp ) {
        this.updateTimestamp = Optional.fromNullable(timestamp  );
        return this;
    }


    @Override
    public Optional<ApplicationScope> getApplicationScope() {

        if(this.withApplicationId.isPresent()){
            return Optional.of(  CpNamingUtils.getApplicationScope( withApplicationId.get()));
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
