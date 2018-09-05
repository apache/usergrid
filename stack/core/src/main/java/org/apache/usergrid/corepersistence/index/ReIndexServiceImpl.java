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


@Singleton
public class ReIndexServiceImpl implements ReIndexService {

    private static final Logger logger = LoggerFactory.getLogger( ReIndexServiceImpl.class );

    private static final MapScope RESUME_MAP_SCOPE =
        new MapScopeImpl( CpNamingUtils.getManagementApplicationId(), "reindexresume" );

    //Keep cursors to resume re-index for 10 days.  This is far beyond it's useful real world implications anyway.
    private static final int INDEX_TTL = 60 * 60 * 24 * 10;

    private static final String MAP_CURSOR_KEY = "cursor";
    private static final String MAP_COUNT_KEY = "count";
    private static final String MAP_STATUS_KEY = "status";
    private static final String MAP_UPDATED_KEY = "lastUpdated";
    private static final String MAP_SEPARATOR = "|||";


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
    public ReIndexServiceImpl( final EntityIndexFactory entityIndexFactory,
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
    public ReIndexStatus rebuildIndex( final ReIndexRequestBuilder reIndexRequestBuilder ) {

        //load our last emitted Scope if a cursor is present

        final AtomicInteger count = new AtomicInteger();

        final Optional<EdgeScope> cursor = parseCursor( reIndexRequestBuilder.getCursor() );


        final CursorSeek<Edge> cursorSeek = getResumeEdge( cursor );

        final Optional<ApplicationScope> appId = reIndexRequestBuilder.getApplicationScope();

        final Optional<Integer> delayTimer = reIndexRequestBuilder.getDelayTimer();

        final Optional<TimeUnit> timeUnitOptional = reIndexRequestBuilder.getTimeUnitOptional();

        Preconditions.checkArgument( !(cursor.isPresent() && appId.isPresent()),
            "You cannot specify an app id and a cursor.  When resuming with cursor you must omit the appid" );

        final Observable<ApplicationScope> applicationScopes = getApplications( cursor, appId );


        final String jobId = StringUtils.sanitizeUUID( UUIDGenerator.newTimeUUID() );

        final long modifiedSince = reIndexRequestBuilder.getUpdateTimestamp().or( Long.MIN_VALUE );

        // create an observable that loads a batch to be indexed

        final boolean isForCollection = reIndexRequestBuilder.getCollectionName().isPresent();

        if(isForCollection) {

            String collectionName =  InflectionUtils.pluralize(
                CpNamingUtils.getNameFromEdgeType(reIndexRequestBuilder.getCollectionName().get() ));

            CollectionSettings collectionSettings =
                collectionSettingsFactory.getInstance( new CollectionSettingsScopeImpl(appId.get().getApplication(), collectionName) );

            Optional<Map<String, Object>> existingSettings =
                collectionSettings.getCollectionSettings( collectionName );

            // If we do have a schema then parse it and add it to a list of properties we want to keep.Otherwise return.
            if ( existingSettings.isPresent() ) {

                Map jsonMapData = existingSettings.get();

                jsonMapData.put( "lastReindexed", Instant.now().toEpochMilli() );

                // should probably roll this into the cache.
                collectionSettings.putCollectionSettings(
                    collectionName, JsonUtils.mapToJsonString(jsonMapData ) );
            }

        }

        allEntityIdsObservable.getEdgesToEntities( applicationScopes,
            reIndexRequestBuilder.getCollectionName(), cursorSeek.getSeekValue() )
            .buffer( indexProcessorFig.getReindexBufferSize())
            .doOnNext( edgeScopes -> {
                logger.info("Sending batch of {} to be indexed.", edgeScopes.size());
                indexService.indexBatch(edgeScopes, modifiedSince, AsyncEventQueueType.UTILITY);
                count.addAndGet(edgeScopes.size() );
                if( edgeScopes.size() > 0 ) {
                    writeCursorState(jobId, edgeScopes.get(edgeScopes.size() - 1));
                }
                if( isForCollection ){
                    writeStateMetaForCollection(
                        appId.get().getApplication().getUuid().toString(),
                        reIndexRequestBuilder.getCollectionName().get(),
                        Status.INPROGRESS, count.get(),
                        System.currentTimeMillis() );
                }else{
                    writeStateMeta( jobId, Status.INPROGRESS, count.get(), System.currentTimeMillis() );
                }
            })
            .doOnCompleted(() ->{
                if( isForCollection ){
                    writeStateMetaForCollection(
                        appId.get().getApplication().getUuid().toString(),
                        reIndexRequestBuilder.getCollectionName().get(),
                        Status.COMPLETE, count.get(),
                        System.currentTimeMillis() );
                }else {
                    writeStateMeta(jobId, Status.COMPLETE, count.get(), System.currentTimeMillis());
                }
            })
            .subscribeOn( Schedulers.io() ).subscribe();

        if(isForCollection){
            return new ReIndexStatus( "", Status.STARTED, 0, 0, CpNamingUtils.getNameFromEdgeType(reIndexRequestBuilder.getCollectionName().get()) );

        }


        return new ReIndexStatus( jobId, Status.STARTED, 0, 0, "" );
    }


    @Override
    public ReIndexRequestBuilder getBuilder() {
        return new ReIndexRequestBuilderImpl();
    }


    @Override
    public ReIndexStatus getStatus( final String jobId ) {
        Preconditions.checkNotNull( jobId, "jobId must not be null" );
        return getIndexResponse( jobId );
    }

    @Override
    public ReIndexStatus getStatusForCollection( final String appIdString, final String collectionName ) {
        Preconditions.checkNotNull( collectionName, "appIdString must not be null" );
        Preconditions.checkNotNull( collectionName, "collectionName must not be null" );
        return getIndexResponseForCollection( appIdString, collectionName );
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
     * Generate an observable for our appliation scope
     */
    private Observable<ApplicationScope> getApplications( final Optional<EdgeScope> cursor,
                                                          final Optional<ApplicationScope> appId ) {
        //cursor is present use it and skip until we hit that app
        if (cursor.isPresent()) {

            final EdgeScope cursorValue = cursor.get();
            //we have a cursor and an application scope that was used.
            return allApplicationsObservable.getData().skipWhile(
                applicationScope -> !cursorValue.getApplicationScope().equals(applicationScope));
        }
        //this is intentional.  If
        else if (appId.isPresent()) {
            return Observable.just(appId.get())
                .doOnNext(appScope -> {
                    //make sure index is initialized on rebuild
                    entityIndexFactory.createEntityIndex(
                        indexLocationStrategyFactory.getIndexLocationStrategy(appScope)
                    ).initialize();
                });
        }

        return allApplicationsObservable.getData()
            .doOnNext(appScope -> {
                //make sure index is initialized on rebuild
                entityIndexFactory.createEntityIndex(
                    indexLocationStrategyFactory.getIndexLocationStrategy(appScope)
                ).initialize();
            });
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

        mapManager.putString( jobId + MAP_CURSOR_KEY, serializedState, INDEX_TTL );
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
    private ReIndexStatus getIndexResponse( final String jobId ) {

        final String stringStatus = mapManager.getString( jobId+MAP_STATUS_KEY );

        if(stringStatus == null){
           return new ReIndexStatus( jobId, Status.UNKNOWN, 0, 0, "" );
        }

        final Status status = Status.valueOf( stringStatus );

        final long processedCount = mapManager.getLong( jobId + MAP_COUNT_KEY );
        final long lastUpdated = mapManager.getLong( jobId + MAP_UPDATED_KEY );

        return new ReIndexStatus( jobId, status, processedCount, lastUpdated, "" );
    }


    private void writeStateMetaForCollection(final String appIdString, final String collectionName,
                                             final Status status, final long processedCount, final long lastUpdated ) {

    	String prefixedColName = CpNamingUtils.getEdgeTypeFromCollectionName( collectionName.toLowerCase() );
    	if(logger.isDebugEnabled()) {
            logger.debug( "Flushing state for collection {}, status {}, processedCount {}, lastUpdated {}",
            		collectionName, status, processedCount, lastUpdated);
        }

        mapManager.putString( appIdString + MAP_SEPARATOR + prefixedColName + MAP_STATUS_KEY, status.name() );
        mapManager.putLong( appIdString + MAP_SEPARATOR + prefixedColName + MAP_COUNT_KEY, processedCount );
        mapManager.putLong( appIdString + MAP_SEPARATOR + prefixedColName + MAP_UPDATED_KEY, lastUpdated );
    }


    private ReIndexStatus getIndexResponseForCollection( final String appIdString, final String collectionName ) {

        String prefixedColName = CpNamingUtils.getEdgeTypeFromCollectionName( collectionName.toLowerCase() );
    	final String stringStatus =
            mapManager.getString( appIdString + MAP_SEPARATOR + prefixedColName + MAP_STATUS_KEY );

        if(stringStatus == null){
            return new ReIndexStatus( "", Status.UNKNOWN, 0, 0, collectionName );
        }

        final Status status = Status.valueOf( stringStatus );

        final long processedCount = mapManager.getLong( appIdString + MAP_SEPARATOR + prefixedColName + MAP_COUNT_KEY );
        final long lastUpdated = mapManager.getLong( appIdString + MAP_SEPARATOR + prefixedColName + MAP_UPDATED_KEY );

        return new ReIndexStatus( "", status, processedCount, lastUpdated, collectionName );
    }
}


