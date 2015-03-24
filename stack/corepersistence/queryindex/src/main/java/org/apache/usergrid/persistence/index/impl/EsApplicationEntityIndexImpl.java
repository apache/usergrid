/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */
package org.apache.usergrid.persistence.index.impl;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.index.*;
import org.apache.usergrid.persistence.index.query.CandidateResult;
import org.apache.usergrid.persistence.index.query.CandidateResults;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.map.MapManagerFactory;
import org.apache.usergrid.persistence.map.MapScope;
import org.apache.usergrid.persistence.map.impl.MapScopeImpl;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.deletebyquery.IndexDeleteByQueryResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.apache.usergrid.persistence.index.impl.IndexingUtils.*;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.SPLITTER;

/**
 * Classy class class.
 */
public class EsApplicationEntityIndexImpl implements ApplicationEntityIndex{

    private static final Logger logger = LoggerFactory.getLogger(EsApplicationEntityIndexImpl.class);

    private final ApplicationScope applicationScope;
    private final IndexIdentifier indexIdentifier;
    private final Timer searchTimer;
    private final Timer cursorTimer;
    private final MapManager mapManager;
    private final AliasedEntityIndex entityIndex;
    private final IndexBufferProducer indexBatchBufferProducer;
    private final EsIndexCache indexCache;
    private final IndexFig indexFig;
    private final EsProvider esProvider;
    private final IndexIdentifier.IndexAlias alias;
    private final Timer deleteApplicationTimer;
    private final Meter deleteApplicationMeter;
    private FailureMonitor failureMonitor;
    private final int cursorTimeout;
    @Inject
    public EsApplicationEntityIndexImpl(@Assisted ApplicationScope appScope, final AliasedEntityIndex entityIndex,  final IndexFig config,
                                        final IndexBufferProducer indexBatchBufferProducer, final EsProvider provider,
                                        final EsIndexCache indexCache, final MetricsFactory metricsFactory,
                                        final MapManagerFactory mapManagerFactory, final IndexFig indexFig, final IndexIdentifier indexIdentifier){
        this.entityIndex = entityIndex;
        this.indexBatchBufferProducer = indexBatchBufferProducer;
        this.indexCache = indexCache;
        this.indexFig = indexFig;
        this.indexIdentifier = indexIdentifier;
        ValidationUtils.validateApplicationScope(appScope);
        this.applicationScope = appScope;
        final MapScope mapScope = new MapScopeImpl(appScope.getApplication(), "cursorcache");
        this.failureMonitor = new FailureMonitorImpl(config, provider);
        this.esProvider = provider;

        mapManager = mapManagerFactory.createMapManager(mapScope);
        this.searchTimer = metricsFactory
            .getTimer(EsApplicationEntityIndexImpl.class, "search.timer");
        this.cursorTimer = metricsFactory
            .getTimer(EsApplicationEntityIndexImpl.class, "search.cursor.timer");
        this.cursorTimeout = config.getQueryCursorTimeout();

        this.deleteApplicationTimer = metricsFactory
            .getTimer(EsApplicationEntityIndexImpl.class, "delete.application");
        this.deleteApplicationMeter = metricsFactory
            .getMeter(EsApplicationEntityIndexImpl.class, "delete.application.meter");

        this.alias = indexIdentifier.getAlias();

    }

    @Override
    public void initializeIndex() {
        final int numberOfShards = indexFig.getNumberOfShards();
        final int numberOfReplicas = indexFig.getNumberOfReplicas();
        indexCache.invalidate(alias);
        String[] indexes = entityIndex.getUniqueIndexes();
        if(indexes == null || indexes.length==0) {
            entityIndex.addIndex(null, numberOfShards, numberOfReplicas, indexFig.getWriteConsistencyLevel());
        }

    }

    @Override
    public EntityIndexBatch createBatch() {
        EntityIndexBatch batch = new EsEntityIndexBatchImpl(
            applicationScope, indexBatchBufferProducer, entityIndex, indexIdentifier );
        return batch;
    }

    @Override
    public CandidateResults search(final IndexScope indexScope, final SearchTypes searchTypes,
                                   final Query query ) {

        SearchResponse searchResponse;

        if ( query.getCursor() == null ) {
            SearchRequestBuilder srb = esProvider.getClient().prepareSearch( alias.getReadAlias() )
                .setTypes(searchTypes.getTypeNames(applicationScope))
                .setScroll(cursorTimeout + "m")
                .setQuery(query.createQueryBuilder(createContextName(applicationScope, indexScope)));

            final FilterBuilder fb = query.createFilterBuilder();

            //we have post filters, apply them
            if ( fb != null ) {
                logger.debug( "   Filter: {} ", fb.toString() );
                srb = srb.setPostFilter( fb );
            }


            srb = srb.setFrom( 0 ).setSize( query.getLimit() );

            for ( Query.SortPredicate sp : query.getSortPredicates() ) {

                final SortOrder order;
                if ( sp.getDirection().equals( Query.SortDirection.ASCENDING ) ) {
                    order = SortOrder.ASC;
                }
                else {
                    order = SortOrder.DESC;
                }

                // we do not know the type of the "order by" property and so we do not know what
                // type prefix to use. So, here we add an order by clause for every possible type
                // that you can order by: string, number and boolean and we ask ElasticSearch
                // to ignore any fields that are not present.

                final String stringFieldName = STRING_PREFIX + sp.getPropertyName();
                final FieldSortBuilder stringSort = SortBuilders.fieldSort(stringFieldName)
                    .order( order ).ignoreUnmapped( true );
                srb.addSort( stringSort );

                logger.debug( "   Sort: {} order by {}", stringFieldName, order.toString() );

                final String longFieldName = LONG_PREFIX + sp.getPropertyName();
                final FieldSortBuilder longSort = SortBuilders.fieldSort( longFieldName )
                    .order( order ).ignoreUnmapped( true );
                srb.addSort( longSort );
                logger.debug( "   Sort: {} order by {}", longFieldName, order.toString() );


                final String doubleFieldName = DOUBLE_PREFIX + sp.getPropertyName();
                final FieldSortBuilder doubleSort = SortBuilders.fieldSort( doubleFieldName )
                    .order( order ).ignoreUnmapped( true );
                srb.addSort( doubleSort );
                logger.debug( "   Sort: {} order by {}", doubleFieldName, order.toString() );


                final String booleanFieldName = BOOLEAN_PREFIX + sp.getPropertyName();
                final FieldSortBuilder booleanSort = SortBuilders.fieldSort( booleanFieldName )
                    .order( order ).ignoreUnmapped( true );
                srb.addSort( booleanSort );
                logger.debug( "   Sort: {} order by {}", booleanFieldName, order.toString() );
            }


            if ( logger.isDebugEnabled() ) {
                logger.debug( "Searching index (read alias): {}\n  scope: {} \n type: {}\n   query: {} ",
                    this.alias.getReadAlias(), indexScope.getOwner(), searchTypes.getTypeNames(applicationScope), srb );
            }

            try {
                //Added For Graphite Metrics
                Timer.Context timeSearch = searchTimer.time();
                searchResponse = srb.execute().actionGet();
                timeSearch.stop();
            }
            catch ( Throwable t ) {
                logger.error( "Unable to communicate with Elasticsearch", t );
                failureMonitor.fail( "Unable to execute batch", t );
                throw t;
            }


            failureMonitor.success();
        }
        else {
            String userCursorString = query.getCursor();
            if ( userCursorString.startsWith( "\"" ) ) {
                userCursorString = userCursorString.substring( 1 );
            }
            if ( userCursorString.endsWith( "\"" ) ) {
                userCursorString = userCursorString.substring( 0, userCursorString.length() - 1 );
            }

            //now get the cursor from the map  and validate
            final String esScrollCursor  = mapManager.getString( userCursorString );

            Preconditions.checkArgument(esScrollCursor != null, "Could not find a cursor for the value '{}' ", esScrollCursor);



            logger.debug( "Executing query with cursor: {} ", esScrollCursor );


            SearchScrollRequestBuilder ssrb = esProvider.getClient()
                .prepareSearchScroll(esScrollCursor).setScroll( cursorTimeout + "m" );

            try {
                //Added For Graphite Metrics
                Timer.Context timeSearchCursor = cursorTimer.time();
                searchResponse = ssrb.execute().actionGet();
                timeSearchCursor.stop();
            }
            catch ( Throwable t ) {
                logger.error( "Unable to communicate with elasticsearch", t );
                failureMonitor.fail( "Unable to execute batch", t );
                throw t;
            }


            failureMonitor.success();
        }

        return parseResults(searchResponse, query);
    }

    /**
     * Completely delete an index.
     */
    public Observable deleteApplication() {
        deleteApplicationMeter.mark();
        String idString = IndexingUtils.idString(applicationScope.getApplication());
        final TermQueryBuilder tqb = QueryBuilders.termQuery(APPLICATION_ID_FIELDNAME, idString);
        final String[] indexes = entityIndex.getUniqueIndexes();
        Timer.Context timer = deleteApplicationTimer.time();
        //Added For Graphite Metrics
        return Observable.from(indexes)
            .flatMap(index -> {

                final ListenableActionFuture<DeleteByQueryResponse> response = esProvider.getClient()
                    .prepareDeleteByQuery(alias.getWriteAlias()).setQuery(tqb).execute();

                response.addListener(new ActionListener<DeleteByQueryResponse>() {

                    @Override
                    public void onResponse(DeleteByQueryResponse response) {
                        checkDeleteByQueryResponse(tqb, response);
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        logger.error("failed on delete index", e);
                    }
                });
                return Observable.from(response);
            })
            .doOnError( t -> logger.error("Failed on delete application",t))
            .doOnCompleted(() -> timer.stop());
    }

    /**
     * Validate the response doesn't contain errors, if it does, fail fast at the first error we encounter
     */
    private void checkDeleteByQueryResponse(
        final QueryBuilder query, final DeleteByQueryResponse response ) {

        for ( IndexDeleteByQueryResponse indexDeleteByQueryResponse : response ) {
            final ShardOperationFailedException[] failures = indexDeleteByQueryResponse.getFailures();

            for ( ShardOperationFailedException failedException : failures ) {
                logger.error( String.format("Unable to delete by query %s. "
                            + "Failed with code %d and reason %s on shard %s in index %s",
                        query.toString(),
                        failedException.status().getStatus(),
                        failedException.reason(),
                        failedException.shardId(),
                        failedException.index() )
                );
            }

        }
    }




    private CandidateResults parseResults( final SearchResponse searchResponse, final Query query ) {

        final SearchHits searchHits = searchResponse.getHits();
        final SearchHit[] hits = searchHits.getHits();
        final int length = hits.length;

        logger.debug("   Hit count: {} Total hits: {}", length, searchHits.getTotalHits());

        List<CandidateResult> candidates = new ArrayList<>( length );

        for ( SearchHit hit : hits ) {

            String[] idparts = hit.getId().split( SPLITTER );
            String id = idparts[0];
            String type = idparts[1];
            String version = idparts[2];

            Id entityId = new SimpleId( UUID.fromString(id), type );

            candidates.add( new CandidateResult( entityId, UUID.fromString( version ) ) );
        }

        CandidateResults candidateResults = new CandidateResults( query, candidates );

        if ( candidates.size() >= query.getLimit() ) {
            //USERGRID-461 our cursor is getting too large, map it to a new time UUID
            //TODO T.N., this shouldn't live here. This should live at the UG core tier.  However the RM/EM are an absolute mess, so until they're refactored, this is it's home

            final String userCursorString = org.apache.usergrid.persistence.index.utils.StringUtils.sanitizeUUID( UUIDGenerator.newTimeUUID() );

            final String esScrollCursor = searchResponse.getScrollId();

            //now set this into our map module
            final int minutes = indexFig.getQueryCursorTimeout();

            //just truncate it, we'll never hit a long value anyway
            mapManager.putString( userCursorString, esScrollCursor, ( int ) TimeUnit.MINUTES.toSeconds( minutes ) );

            candidateResults.setCursor( userCursorString );
            logger.debug(" User cursor = {},  Cursor = {} ", userCursorString, esScrollCursor);
        }

        return candidateResults;
    }

}
