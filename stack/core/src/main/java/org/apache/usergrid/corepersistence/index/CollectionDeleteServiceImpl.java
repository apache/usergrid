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


import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


import org.apache.usergrid.corepersistence.asyncevents.AsyncEventQueueType;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.asyncevents.AsyncEventService;
import org.apache.usergrid.corepersistence.pipeline.cursor.CursorSerializerUtil;
import org.apache.usergrid.corepersistence.pipeline.read.CursorSeek;
import org.apache.usergrid.corepersistence.rx.impl.AllApplicationsObservable;
import org.apache.usergrid.corepersistence.rx.impl.AllEntityIdsObservable;
import org.apache.usergrid.corepersistence.rx.impl.EdgeScope;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.StringUtils;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.map.MapManagerFactory;
import org.apache.usergrid.persistence.map.MapScope;
import org.apache.usergrid.persistence.map.impl.MapScopeImpl;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.utils.InflectionUtils;
import org.apache.usergrid.utils.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;
import rx.schedulers.Schedulers;

import static com.google.common.base.Optional.fromNullable;


@Singleton
public class CollectionDeleteServiceImpl implements CollectionDeleteService {

    private static final Logger logger = LoggerFactory.getLogger( CollectionDeleteServiceImpl.class );

    private static final MapScope RESUME_MAP_SCOPE =
        new MapScopeImpl( CpNamingUtils.getManagementApplicationId(), "collectiondeleteresume" );

    //Keep cursors to resume collection delete for 10 days.
    private static final int CURSOR_TTL = 60 * 60 * 24 * 10;

    private static final String MAP_CURSOR_KEY = "cursor";
    private static final String MAP_COUNT_KEY = "count";
    private static final String MAP_STATUS_KEY = "status";
    private static final String MAP_UPDATED_KEY = "lastUpdated";


    private final AllApplicationsObservable allApplicationsObservable;
    private final IndexLocationStrategyFactory indexLocationStrategyFactory;
    private final AllEntityIdsObservable allEntityIdsObservable;
    private final IndexProcessorFig indexProcessorFig;
    private final MapManager mapManager;
    private final MapManagerFactory mapManagerFactory;
    private final AsyncEventService indexService;
    private final EntityIndexFactory entityIndexFactory;
    private final CollectionSettingsFactory collectionSettingsFactory;


    @Inject
    public CollectionDeleteServiceImpl(final EntityIndexFactory entityIndexFactory,
                                       final IndexLocationStrategyFactory indexLocationStrategyFactory,
                                       final AllEntityIdsObservable allEntityIdsObservable,
                                       final MapManagerFactory mapManagerFactory,
                                       final AllApplicationsObservable allApplicationsObservable,
                                       final IndexProcessorFig indexProcessorFig,
                                       final CollectionSettingsFactory collectionSettingsFactory,
                                       final AsyncEventService indexService ) {
        this.entityIndexFactory = entityIndexFactory;
        this.indexLocationStrategyFactory = indexLocationStrategyFactory;
        this.allEntityIdsObservable = allEntityIdsObservable;
        this.allApplicationsObservable = allApplicationsObservable;
        this.indexProcessorFig = indexProcessorFig;
        this.indexService = indexService;
        this.collectionSettingsFactory = collectionSettingsFactory;
        this.mapManagerFactory = mapManagerFactory;
        this.mapManager = mapManagerFactory.createMapManager( RESUME_MAP_SCOPE );
    }


    //TODO: optional delay, param.
    @Override
    public CollectionDeleteStatus deleteCollection( final CollectionDeleteRequestBuilder collectionDeleteRequestBuilder) {

        final AtomicInteger count = new AtomicInteger();

        final Optional<EdgeScope> cursor = parseCursor( collectionDeleteRequestBuilder.getCursor() );

        final CursorSeek<Edge> cursorSeek = getResumeEdge( cursor );

        final Optional<Integer> delayTimer = collectionDeleteRequestBuilder.getDelayTimer();

        final Optional<TimeUnit> timeUnitOptional = collectionDeleteRequestBuilder.getTimeUnitOptional();

        Optional<ApplicationScope> appId = collectionDeleteRequestBuilder.getApplicationScope();

        Preconditions.checkArgument(collectionDeleteRequestBuilder.getCollectionName().isPresent(),
            "You must specify a collection name");
        String collectionName = collectionDeleteRequestBuilder.getCollectionName().get();

        Preconditions.checkArgument( !(cursor.isPresent() && appId.isPresent()),
            "You cannot specify an app id and a cursor.  When resuming with cursor you must omit the appid." );
        Preconditions.checkArgument( cursor.isPresent() || appId.isPresent(),
            "Either application ID or cursor is required.");

        ApplicationScope applicationScope;
        if (appId.isPresent()) {
            applicationScope = appId.get();
        } else { // cursor is present
            applicationScope = cursor.get().getApplicationScope();
        }


        final String jobId = StringUtils.sanitizeUUID( UUIDGenerator.newTimeUUID() );

        // default to current time
        final long endTimestamp = collectionDeleteRequestBuilder.getEndTimestamp().or( System.currentTimeMillis() );

        String pluralizedCollectionName = InflectionUtils.pluralize(CpNamingUtils.getNameFromEdgeType(collectionName));

        CollectionSettings collectionSettings =
            collectionSettingsFactory.getInstance(new CollectionSettingsScopeImpl(applicationScope.getApplication(), pluralizedCollectionName));

        Optional<Map<String, Object>> existingSettings =
            collectionSettings.getCollectionSettings( pluralizedCollectionName );

        if ( existingSettings.isPresent() ) {

            Map jsonMapData = existingSettings.get();

            jsonMapData.put( "lastCollectionClear", Instant.now().toEpochMilli() );

            collectionSettings.putCollectionSettings(
                pluralizedCollectionName, JsonUtils.mapToJsonString(jsonMapData ) );
        }

        allEntityIdsObservable.getEdgesToEntities( Observable.just(applicationScope),
            fromNullable(collectionName), cursorSeek.getSeekValue() )
            .buffer( indexProcessorFig.getCollectionDeleteBufferSize())
            .doOnNext( edgeScopes -> {
                logger.info("Sending batch of {} to be deleted.", edgeScopes.size());
                indexService.deleteBatch(edgeScopes, endTimestamp, AsyncEventQueueType.DELETE);
                count.addAndGet(edgeScopes.size() );
                if( edgeScopes.size() > 0 ) {
                    writeCursorState(jobId, edgeScopes.get(edgeScopes.size() - 1));
                }
                writeStateMeta( jobId, Status.INPROGRESS, count.get(), System.currentTimeMillis() ); })
            .doOnCompleted(() -> writeStateMeta( jobId, Status.COMPLETE, count.get(), System.currentTimeMillis() ))
            .subscribeOn( Schedulers.io() ).subscribe();


        return new CollectionDeleteStatus( jobId, Status.STARTED, 0, 0 );
    }


    @Override
    public CollectionDeleteRequestBuilder getBuilder() {
        return new CollectionDeleteRequestBuilderImpl();
    }


    @Override
    public CollectionDeleteStatus getStatus( final String jobId ) {
        Preconditions.checkNotNull( jobId, "jobId must not be null" );
        return getCollectionDeleteResponse( jobId );
    }


    /**
     * Get the resume edge scope
     *
     * @param edgeScope The optional edge scope from the cursor
     */
    private CursorSeek<Edge> getResumeEdge( final Optional<EdgeScope> edgeScope ) {


        if ( edgeScope.isPresent() ) {
            return new CursorSeek<>( Optional.of( edgeScope.get().getEdge() ) );
        }

        return new CursorSeek<>( Optional.absent() );
    }


    /**
     * Swap our cursor for an optional edgescope
     */
    private Optional<EdgeScope> parseCursor( final Optional<String> cursor ) {

        if ( !cursor.isPresent() ) {
            return Optional.absent();
        }

        //get our cursor
        final String persistedCursor = mapManager.getString( cursor.get() );

        if ( persistedCursor == null ) {
            return Optional.absent();
        }

        final JsonNode node = CursorSerializerUtil.fromString( persistedCursor );

        final EdgeScope edgeScope = EdgeScopeSerializer.INSTANCE.fromJsonNode( node, CursorSerializerUtil.getMapper() );

        return Optional.of( edgeScope );
    }


    /**
     * Write the cursor state to the map in cassandra
     */
    private void writeCursorState( final String jobId, final EdgeScope edge ) {

        final JsonNode node = EdgeScopeSerializer.INSTANCE.toNode( CursorSerializerUtil.getMapper(), edge );

        final String serializedState = CursorSerializerUtil.asString( node );

        mapManager.putString( jobId + MAP_CURSOR_KEY, serializedState, CURSOR_TTL);
    }


    /**
     * Write our state meta data into cassandra so everyone can see it
     * @param jobId
     * @param status
     * @param processedCount
     * @param lastUpdated
     */
    private void writeStateMeta( final String jobId, final Status status, final long processedCount,
                                 final long lastUpdated ) {

        if(logger.isDebugEnabled()) {
            logger.debug( "Flushing state for jobId {}, status {}, processedCount {}, lastUpdated {}",
                    jobId, status, processedCount, lastUpdated);
        }

        mapManager.putString( jobId + MAP_STATUS_KEY, status.name() );
        mapManager.putLong( jobId + MAP_COUNT_KEY, processedCount );
        mapManager.putLong( jobId + MAP_UPDATED_KEY, lastUpdated );
    }


    /**
     * Get the index response from the jobId
     * @param jobId
     * @return
     */
    private CollectionDeleteStatus getCollectionDeleteResponse( final String jobId ) {

        final String stringStatus = mapManager.getString( jobId+MAP_STATUS_KEY );

        if(stringStatus == null){
           return new CollectionDeleteStatus( jobId, Status.UNKNOWN, 0, 0 );
        }

        final Status status = Status.valueOf( stringStatus );

        final long processedCount = mapManager.getLong( jobId + MAP_COUNT_KEY );
        final long lastUpdated = mapManager.getLong( jobId + MAP_UPDATED_KEY );

        return new CollectionDeleteStatus( jobId, status, processedCount, lastUpdated );
    }
}


