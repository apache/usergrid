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


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.deletebyquery.IndexDeleteByQueryResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.index.AliasedEntityIndex;
import org.apache.usergrid.persistence.index.ApplicationEntityIndex;
import org.apache.usergrid.persistence.index.CandidateResult;
import org.apache.usergrid.persistence.index.CandidateResults;
import org.apache.usergrid.persistence.index.ElasticSearchQueryBuilder.SearchRequestBuilderStrategyV2;
import org.apache.usergrid.persistence.index.EntityIndexBatch;
import org.apache.usergrid.persistence.index.IndexEdge;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.SearchEdge;
import org.apache.usergrid.persistence.index.SearchTypes;
import org.apache.usergrid.persistence.index.query.ParsedQuery;
import org.apache.usergrid.persistence.index.query.ParsedQueryBuilder;
import org.apache.usergrid.persistence.index.utils.IndexValidationUtils;
import org.apache.usergrid.persistence.map.MapManager;
import org.apache.usergrid.persistence.map.MapManagerFactory;
import org.apache.usergrid.persistence.map.MapScope;
import org.apache.usergrid.persistence.map.impl.MapScopeImpl;
import org.apache.usergrid.persistence.model.entity.Id;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import rx.Observable;

import static org.apache.usergrid.persistence.index.impl.IndexingUtils.APPLICATION_ID_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.applicationId;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.parseIndexDocId;


/**
 * Classy class class.
 */
public class EsApplicationEntityIndexImpl implements ApplicationEntityIndex {

    private static final Logger logger = LoggerFactory.getLogger( EsApplicationEntityIndexImpl.class );


    private final ApplicationScope applicationScope;
    private final IndexIdentifier indexIdentifier;
    private final Timer searchTimer;
    private final Timer cursorTimer;
    private final MapManager mapManager;
    private final AliasedEntityIndex entityIndex;
    private final IndexBufferConsumer indexBatchBufferProducer;
    private final IndexFig indexFig;
    private final EsProvider esProvider;
    private final IndexAlias alias;
    private final Timer deleteApplicationTimer;
    private final Meter deleteApplicationMeter;
    private final SearchRequestBuilderStrategy searchRequest;
    private final SearchRequestBuilderStrategyV2 searchRequestBuilderStrategyV2;
    private FailureMonitor failureMonitor;
    private final int cursorTimeout;
    private final long queryTimeout;


    @Inject
    public EsApplicationEntityIndexImpl(  ApplicationScope appScope, final AliasedEntityIndex entityIndex,
                                         final IndexFig config, final IndexBufferConsumer indexBatchBufferProducer,
                                         final EsProvider provider,
                                         final MetricsFactory metricsFactory, final MapManagerFactory mapManagerFactory,
                                         final IndexFig indexFig,
                                         final IndexIdentifier indexIdentifier ) {
        this.entityIndex = entityIndex;
        this.indexBatchBufferProducer = indexBatchBufferProducer;
        this.indexFig = indexFig;
        this.indexIdentifier = indexIdentifier;
        ValidationUtils.validateApplicationScope( appScope );
        this.applicationScope = appScope;
        final MapScope mapScope = new MapScopeImpl( appScope.getApplication(), "cursorcache" );
        this.failureMonitor = new FailureMonitorImpl( config, provider );
        this.esProvider = provider;

        mapManager = mapManagerFactory.createMapManager( mapScope );
        this.searchTimer = metricsFactory.getTimer( EsApplicationEntityIndexImpl.class, "search.timer" );
        this.cursorTimer = metricsFactory.getTimer( EsApplicationEntityIndexImpl.class, "search.cursor.timer" );
        this.cursorTimeout = config.getQueryCursorTimeout();
        this.queryTimeout = config.getWriteTimeout();

        this.deleteApplicationTimer =
                metricsFactory.getTimer( EsApplicationEntityIndexImpl.class, "delete.application" );
        this.deleteApplicationMeter =
                metricsFactory.getMeter( EsApplicationEntityIndexImpl.class, "delete.application.meter" );

        this.alias = indexIdentifier.getAlias();

        this.searchRequest = new SearchRequestBuilderStrategy( esProvider, appScope, alias, cursorTimeout );
        this.searchRequestBuilderStrategyV2 = new SearchRequestBuilderStrategyV2( esProvider, appScope, alias, cursorTimeout  );
    }


    @Override
    public EntityIndexBatch createBatch() {
        EntityIndexBatch batch =
                new EsEntityIndexBatchImpl( applicationScope, indexBatchBufferProducer, entityIndex, indexIdentifier );
        return batch;
    }

    public CandidateResults search( final SearchEdge searchEdge, final SearchTypes searchTypes, final String query,
                                    final int limit, final int offset ) {

        IndexValidationUtils.validateSearchEdge(searchEdge);
        Preconditions.checkNotNull( searchTypes, "searchTypes cannot be null" );
        Preconditions.checkNotNull( query, "query cannot be null" );
        Preconditions.checkArgument( limit > 0, "limit must be > 0" );


        SearchResponse searchResponse;

        final ParsedQuery parsedQuery = ParsedQueryBuilder.build( query );

        final SearchRequestBuilder srb = searchRequest.getBuilder( searchEdge, searchTypes, parsedQuery, limit, offset )
                                                      .setTimeout( TimeValue.timeValueMillis( queryTimeout ) );

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Searching index (read alias): {}\n  nodeId: {}, edgeType: {},  \n type: {}\n   query: {} ",
                    this.alias.getReadAlias(), searchEdge.getNodeId(), searchEdge.getEdgeName(),
                    searchTypes.getTypeNames( applicationScope ), srb );
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

        return parseResults(searchResponse, parsedQuery, limit, offset);
    }


    @Override
    public CandidateResults getAllEdgeDocuments( final IndexEdge edge, final Id entityId ) {
        /**
         * Take a list of IndexEdge, with an entityId
         and query Es directly for matches

         */
        IndexValidationUtils.validateSearchEdge( edge );
        Preconditions.checkNotNull( entityId, "entityId cannot be null" );

        SearchResponse searchResponse;

        List<CandidateResult> candidates = new ArrayList<>();

        final ParsedQuery parsedQuery = ParsedQueryBuilder.build( "select *" );

        final SearchRequestBuilder srb = searchRequestBuilderStrategyV2.getBuilder();

        //I can't just search on the entity Id.

        FilterBuilder entityEdgeFilter = FilterBuilders.termFilter( IndexingUtils.EDGE_NODE_ID_FIELDNAME,
            IndexingUtils.idString( edge.getNodeId() ));

        srb.setPostFilter(entityEdgeFilter);

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Searching for marked versions in index (read alias): {}\n  nodeId: {},\n   query: {} ",
                this.alias.getReadAlias(),entityId, srb );
        }

        try {
            //Added For Graphite Metrics
            Timer.Context timeSearch = searchTimer.time();

            //set the timeout on the scroll cursor to 6 seconds and set the number of values returned per shard to 100.
            //The settings for the scroll aren't tested and so we aren't sure what vlaues would be best in a production enviroment
            //TODO: review this and make them not magic numbers when acking this PR.
            searchResponse = srb.setScroll( new TimeValue( 6000 ) ).setSize( 100 ).execute().actionGet();


            while(true){
                //add search result hits to some sort of running tally of hits.
                candidates = aggregateScrollResults( candidates, searchResponse );

                SearchScrollRequestBuilder ssrb = searchRequestBuilderStrategyV2
                    .getScrollBuilder( searchResponse.getScrollId() )
                    .setScroll( new TimeValue( 6000 ) );

                //TODO: figure out how to log exactly what we're putting into elasticsearch
                //                if ( logger.isDebugEnabled() ) {
                //                    logger.debug( "Scroll search using query: {} ",
                //                        ssrb.toString() );
                //                }

                searchResponse = ssrb.execute().actionGet();

                if (searchResponse.getHits().getHits().length == 0) {
                    break;
                }


            }
            timeSearch.stop();
        }
        catch ( Throwable t ) {
            logger.error( "Unable to communicate with Elasticsearch", t );
            failureMonitor.fail( "Unable to execute batch", t );
            throw t;
        }
        failureMonitor.success();

        return new CandidateResults( candidates, parsedQuery.getSelectFieldMappings());
    }


    @Override
    public CandidateResults getAllEntityVersionsBeforeMarkedVersion( final Id entityId, final UUID markedVersion ) {

        Preconditions.checkNotNull( entityId, "entityId cannot be null" );
        Preconditions.checkNotNull( markedVersion, "markedVersion cannot be null" );
        ValidationUtils.verifyVersion( markedVersion );

        SearchResponse searchResponse;

        List<CandidateResult> candidates = new ArrayList<>();

        final ParsedQuery parsedQuery = ParsedQueryBuilder.build( "select *" );

        final SearchRequestBuilder srb = searchRequestBuilderStrategyV2.getBuilder();

        FilterBuilder entityIdFilter = FilterBuilders.termFilter( IndexingUtils.ENTITY_ID_FIELDNAME,
            IndexingUtils.idString( entityId ) );

        FilterBuilder entityVersionFilter = FilterBuilders.rangeFilter( IndexingUtils.ENTITY_VERSION_FIELDNAME ).lte( markedVersion );

        FilterBuilder andFilter = FilterBuilders.andFilter(entityIdFilter,entityVersionFilter  );

        srb.setPostFilter(andFilter);



        if ( logger.isDebugEnabled() ) {
            logger.debug( "Searching for marked versions in index (read alias): {}\n  nodeId: {},\n   query: {} ",
                this.alias.getReadAlias(),entityId, srb );
        }

        try {
            //Added For Graphite Metrics
            Timer.Context timeSearch = searchTimer.time();

            //set the timeout on the scroll cursor to 6 seconds and set the number of values returned per shard to 100.
            //The settings for the scroll aren't tested and so we aren't sure what vlaues would be best in a production enviroment
            //TODO: review this and make them not magic numbers when acking this PR.
            searchResponse = srb.setScroll( new TimeValue( 6000 ) ).setSize( 100 ).execute().actionGet();

            //list that will hold all of the search hits


            while(true){
                //add search result hits to some sort of running tally of hits.
                candidates = aggregateScrollResults( candidates, searchResponse );

                SearchScrollRequestBuilder ssrb = searchRequestBuilderStrategyV2
                    .getScrollBuilder( searchResponse.getScrollId() )
                    .setScroll( new TimeValue( 6000 ) );

                //TODO: figure out how to log exactly what we're putting into elasticsearch
//                if ( logger.isDebugEnabled() ) {
//                    logger.debug( "Scroll search using query: {} ",
//                        ssrb.toString() );
//                }

                searchResponse = ssrb.execute().actionGet();

                if (searchResponse.getHits().getHits().length == 0) {
                    break;
                }


            }
            timeSearch.stop();
        }
        catch ( Throwable t ) {
            logger.error( "Unable to communicate with Elasticsearch", t );
            failureMonitor.fail( "Unable to execute batch", t );
            throw t;
        }
        failureMonitor.success();

        return new CandidateResults( candidates, parsedQuery.getSelectFieldMappings());
    }


    /**
     * Completely delete an index.
     */
    public Observable deleteApplication() {
        deleteApplicationMeter.mark();
        String idString = applicationId( applicationScope.getApplication() );
        final TermQueryBuilder tqb = QueryBuilders.termQuery( APPLICATION_ID_FIELDNAME, idString );
        final String[] indexes = entityIndex.getUniqueIndexes();
        Timer.Context timer = deleteApplicationTimer.time();
        //Added For Graphite Metrics
        return Observable.from( indexes ).flatMap( index -> {

            final ListenableActionFuture<DeleteByQueryResponse> response =
                    esProvider.getClient().prepareDeleteByQuery( alias.getWriteAlias() ).setQuery( tqb ).execute();

            response.addListener( new ActionListener<DeleteByQueryResponse>() {

                @Override
                public void onResponse( DeleteByQueryResponse response ) {
                    checkDeleteByQueryResponse( tqb, response );
                }


                @Override
                public void onFailure( Throwable e ) {
                    logger.error( "failed on delete index", e );
                }
            } );
            return Observable.from( response );
        } ).doOnError( t -> logger.error( "Failed on delete application", t ) ).doOnCompleted( () -> timer.stop() );
    }


    /**
     * Validate the response doesn't contain errors, if it does, fail fast at the first error we encounter
     */
    private void checkDeleteByQueryResponse( final QueryBuilder query, final DeleteByQueryResponse response ) {

        for ( IndexDeleteByQueryResponse indexDeleteByQueryResponse : response ) {
            final ShardOperationFailedException[] failures = indexDeleteByQueryResponse.getFailures();

            for ( ShardOperationFailedException failedException : failures ) {
                logger.error( String.format( "Unable to delete by query %s. "
                                        + "Failed with code %d and reason %s on shard %s in index %s", query.toString(),
                                failedException.status().getStatus(), failedException.reason(),
                                failedException.shardId(), failedException.index() ) );
            }
        }
    }


    /**
     * Parse the results and return the canddiate results
     */
    private CandidateResults parseResults( final SearchResponse searchResponse, final ParsedQuery query,
                                           final int limit, final int from ) {

        final SearchHits searchHits = searchResponse.getHits();
        final SearchHit[] hits = searchHits.getHits();

        logger.debug( "   Hit count: {} Total hits: {}", hits.length, searchHits.getTotalHits() );

        List<CandidateResult> candidates = new ArrayList<>( hits.length );

        for ( SearchHit hit : hits ) {

            final CandidateResult candidateResult = parseIndexDocId( hit.getId() );

            candidates.add( candidateResult );
        }

        final CandidateResults candidateResults = new CandidateResults( candidates, query.getSelectFieldMappings());

        // >= seems odd.  However if we get an overflow, we need to account for it.
        if (  hits.length >= limit ) {

            candidateResults.initializeOffset( from + limit );

        }

        return candidateResults;
    }

    private List<CandidateResult> aggregateScrollResults( List<CandidateResult> candidates,
                                                          final SearchResponse searchResponse ){

        final SearchHits searchHits = searchResponse.getHits();
        final SearchHit[] hits = searchHits.getHits();

        for ( SearchHit hit : hits ) {

            final CandidateResult candidateResult = parseIndexDocId( hit.getId() );

            candidates.add( candidateResult );
        }

        logger.debug( "Aggregated {} out of {} hits ",candidates.size(),searchHits.getTotalHits() );

        return  candidates;

    }

}
