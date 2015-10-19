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
package org.apache.usergrid.persistence.index.impl;


import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.google.common.base.*;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.metrics.ObservableTimer;
import org.apache.usergrid.persistence.core.migration.data.VersionedData;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.Health;
import org.apache.usergrid.persistence.core.util.StringUtils;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.index.*;
import org.apache.usergrid.persistence.index.ElasticSearchQueryBuilder.SearchRequestBuilderStrategyV2;
import org.apache.usergrid.persistence.index.exceptions.IndexException;
import org.apache.usergrid.persistence.index.migration.IndexDataVersions;
import org.apache.usergrid.persistence.index.query.ParsedQuery;
import org.apache.usergrid.persistence.index.query.ParsedQueryBuilder;
import org.apache.usergrid.persistence.index.utils.IndexValidationUtils;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.deletebyquery.IndexDeleteByQueryResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;
import org.elasticsearch.search.aggregations.metrics.sum.SumBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import static org.apache.usergrid.persistence.index.impl.IndexingUtils.APPLICATION_ID_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.applicationId;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.parseIndexDocId;


/**
 * Implements index using ElasticSearch Java API.
 */
@Singleton
public class EsEntityIndexImpl implements EntityIndex,VersionedData {

    private static final Logger logger = LoggerFactory.getLogger( EsEntityIndexImpl.class );

    private final IndexAlias alias;
    private final IndexFig indexFig;
    private final IndexLocationStrategy indexLocationStrategy;
    private final Timer addTimer;
    private final Timer updateAliasTimer;
    private final Timer searchTimer;

    /**
     * We purposefully make this per instance. Some indexes may work, while others may fail
     */
    private final EsProvider esProvider;

    //number of times to wait for the index to refresh properly.
    private static final int MAX_WAITS = 10;
    //number of milliseconds to try again before sleeping
    private static final int WAIT_TIME = 250;

    private static final String VERIFY_TYPE = "entity";

    private static final ImmutableMap<String, Object> DEFAULT_PAYLOAD =
            ImmutableMap.<String, Object>builder().put(IndexingUtils.ENTITY_ID_FIELDNAME, UUIDGenerator.newTimeUUID().toString()).build();


    private final ApplicationScope applicationScope;
    private final SearchRequestBuilderStrategy searchRequest;
    private final SearchRequestBuilderStrategyV2 searchRequestBuilderStrategyV2;
    private final int cursorTimeout;
    private final long queryTimeout;
    private final FailureMonitorImpl failureMonitor;
    private final Timer aggregationTimer;
    private final Timer refreshTimer;

    private IndexCache aliasCache;
    private Timer mappingTimer;
    private Meter refreshIndexMeter;


    @Inject
    public EsEntityIndexImpl( final EsProvider provider,
                              final IndexCache indexCache,
                              final IndexFig indexFig,
                              final MetricsFactory metricsFactory,
                              final IndexLocationStrategy indexLocationStrategy
    ) {

        this.indexFig = indexFig;
        this.indexLocationStrategy = indexLocationStrategy;
        this.failureMonitor = new FailureMonitorImpl( indexFig, provider );
        this.esProvider = provider;
        this.alias = indexLocationStrategy.getAlias();
        this.aliasCache = indexCache;
        this.applicationScope = indexLocationStrategy.getApplicationScope();
        this.cursorTimeout = indexFig.getQueryCursorTimeout();
        this.queryTimeout = indexFig.getWriteTimeout();
        this.searchRequest
            = new SearchRequestBuilderStrategy(esProvider, applicationScope, alias, cursorTimeout );
        this.searchRequestBuilderStrategyV2 = new SearchRequestBuilderStrategyV2( esProvider, applicationScope, alias, cursorTimeout  );

        this.addTimer = metricsFactory.getTimer(EsEntityIndexImpl.class, "index.add");
        this.updateAliasTimer = metricsFactory.getTimer(EsEntityIndexImpl.class, "index.update_alias");
        this.mappingTimer = metricsFactory.getTimer(EsEntityIndexImpl.class, "index.create_mapping");
        this.refreshIndexMeter = metricsFactory.getMeter(EsEntityIndexImpl.class, "index.refresh_index");
        this.searchTimer = metricsFactory.getTimer(EsEntityIndexImpl.class, "search");
        this.aggregationTimer = metricsFactory.getTimer( EsEntityIndexImpl.class, "aggregations" );
        this.refreshTimer = metricsFactory.getTimer( EsEntityIndexImpl.class, "index.refresh" );

    }

    @Override
    public void initialize() {
        final int numberOfShards = indexLocationStrategy.getNumberOfShards();
        final int numberOfReplicas = indexLocationStrategy.getNumberOfReplicas();

        aliasCache.invalidate(alias);
        if (shouldInitialize()) {
            addIndex( indexLocationStrategy.getIndexInitialName(), numberOfShards, numberOfReplicas, indexFig.getWriteConsistencyLevel() );
        }
    }

    /**
     * if there are aliases then we must have an index...weak knowledge
     * @return
     */
    private boolean shouldInitialize() {
        String[] writes = getIndexes(AliasType.Write);
        return writes.length==0;
    }

    @Override
    public void addIndex(final String indexName,
                         final int numberOfShards,
                         final int numberOfReplicas,
                         final String writeConsistency
    ) {
        try {
            //get index name with bucket attached
            Preconditions.checkNotNull(indexName,"must have an indexname");

            Preconditions.checkArgument(!indexName.contains("alias"),indexName + " name cannot contain alias " );

            //Create index
            try {
                final AdminClient admin = esProvider.getClient().admin();
                Settings settings = ImmutableSettings.settingsBuilder()
                    .put("index.number_of_shards", numberOfShards)
                    .put("index.number_of_replicas", numberOfReplicas)
                        //dont' allow unmapped queries, and don't allow dynamic mapping
                    .put("index.query.parse.allow_unmapped_fields", false)
                    .put("index.mapper.dynamic", false)
                    .put("action.write_consistency", writeConsistency)
                    .build();

                //Added For Graphite Metrics
                Timer.Context timeNewIndexCreation = addTimer.time();
                final CreateIndexResponse cir = admin.indices().prepareCreate(indexName)
                        .setSettings(settings)
                    .execute()
                        .actionGet();
                timeNewIndexCreation.stop();

                //create the mappings
                createMappings( indexName );

                //ONLY add the alias if we create the index, otherwise we're going to overwrite production settings


                logger.info("Created new Index Name [{}] ACK=[{}]", indexName, cir.isAcknowledged());
            } catch (IndexAlreadyExistsException e) {
                logger.info("Index Name [{}] already exists", indexName);
            }
            /**
             * DO NOT MOVE THIS LINE OF CODE UNLESS YOU REALLY KNOW WHAT YOU'RE DOING!!!!
             */

            //We do NOT want to create an alias if the index already exists, we'll overwrite the indexes that
            //may have been set via other administrative endpoint

            addAlias(indexName);

            testNewIndex();

        } catch (IndexAlreadyExistsException expected) {
            // this is expected to happen if index already exists, it's a no-op and swallow
        } catch (IOException e) {
            throw new RuntimeException("Unable to initialize index", e);
        }
    }

    private void addAlias(String indexName) {
        Timer.Context timer = updateAliasTimer.time();
        try {
            Boolean isAck;

            final AdminClient adminClient = esProvider.getClient().admin();

            String[] indexNames = getIndexes(AliasType.Write);

            int count = 0;
            IndicesAliasesRequestBuilder aliasesRequestBuilder = adminClient.indices().prepareAliases();
            for (String currentIndex : indexNames) {
                aliasesRequestBuilder.removeAlias(currentIndex, alias.getWriteAlias());
                count++;
            }
            if (count > 0) {
                isAck = aliasesRequestBuilder.execute().actionGet().isAcknowledged();
                logger.info("Removed Index Name from Alias=[{}] ACK=[{}]", alias, isAck);
            }
            aliasesRequestBuilder = adminClient.indices().prepareAliases();
            //Added For Graphite Metrics
            //add write alias
            aliasesRequestBuilder.addAlias(indexName, alias.getWriteAlias());
            //Added For Graphite Metrics
            // add read alias
            aliasesRequestBuilder.addAlias(indexName, alias.getReadAlias());

            isAck = aliasesRequestBuilder.execute().actionGet().isAcknowledged();
            logger.info("Created new read and write aliases ACK=[{}]", isAck);
            aliasCache.invalidate(alias);

        } catch (Exception e) {
            logger.warn("Failed to create alias ", e);
        } finally {
            timer.stop();
        }
    }

    @Override
    public String[] getIndexes(final AliasType aliasType) {
        return aliasCache.getIndexes(alias, aliasType);
    }


    /**
     * Tests writing a document to a new index to ensure it's working correctly. See this post:
     * http://s.apache.org/index-missing-exception
     */
    private void testNewIndex() {

        // create the document, this ensures the index is ready
        // Immediately create a document and remove it to ensure the entire cluster is ready
        // to receive documents. Occasionally we see errors.
        // See this post: http://s.apache.org/index-missing-exception

        logger.debug("Testing new index name: read {} write {}", alias.getReadAlias(), alias.getWriteAlias());

        final RetryOperation retryOperation = () -> {
            final String tempId = UUIDGenerator.newTimeUUID().toString();

            esProvider.getClient().prepareIndex( alias.getWriteAlias(), VERIFY_TYPE, tempId )
                 .setSource(DEFAULT_PAYLOAD).get();

            logger.info( "Successfully created new document with docId {} "
                 + "in index read {} write {} and type {}",
                    tempId, alias.getReadAlias(), alias.getWriteAlias(), VERIFY_TYPE );

            // delete all types, this way if we miss one it will get cleaned up
            esProvider.getClient().prepareDelete( alias.getWriteAlias(), VERIFY_TYPE, tempId).get();

            logger.info( "Successfully deleted  documents in read {} write {} and type {} with id {}",
                    alias.getReadAlias(), alias.getWriteAlias(), VERIFY_TYPE, tempId );

            return true;
        };

        doInRetry(retryOperation);
    }


    /**
     * Setup ElasticSearch type mappings as a template that applies to all new indexes.
     * Applies to all indexes that* start with our prefix.
     */
    private void createMappings(final String indexName) throws IOException {


        //Added For Graphite Metrics
        Timer.Context timePutIndex = mappingTimer.time();
        PutMappingResponse  pitr = esProvider.getClient().admin().indices().preparePutMapping( indexName ).setType( "entity" ).setSource(
                getMappingsContent() ).execute().actionGet();
        timePutIndex.stop();
        if ( !pitr.isAcknowledged() ) {
            throw new IndexException( "Unable to create default mappings" );
        }
    }


    /**
     * Get the content from our mappings file
     * @return
     */
    private String getMappingsContent(){
        URL url = Resources.getResource("org/apache/usergrid/persistence/index/usergrid-mappings.json");
        try {
            return Resources.toString(url, Charsets.UTF_8);
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Unable to read mappings file", e );
        }
    }



    public Observable<IndexRefreshCommandInfo> refreshAsync() {

        refreshIndexMeter.mark();
        final long start = System.currentTimeMillis();

        String[] indexes = getIndexes();
        if (indexes.length == 0) {
            logger.debug("Not refreshing indexes. none found");
        }
        //Added For Graphite Metrics
        RefreshResponse response =
            esProvider.getClient().admin().indices().prepareRefresh(indexes).execute().actionGet();
        int failedShards = response.getFailedShards();
        int successfulShards = response.getSuccessfulShards();
        ShardOperationFailedException[] sfes = response.getShardFailures();
        if (sfes != null) {
            for (ShardOperationFailedException sfe : sfes) {
                logger.error("Failed to refresh index:{} reason:{}", sfe.index(), sfe.reason());
            }
        }
        logger.debug("Refreshed indexes: {},success:{} failed:{} ", StringUtils.join(indexes, ", "),
            successfulShards, failedShards);

        IndexRefreshCommandInfo refreshResults = new IndexRefreshCommandInfo(failedShards == 0,
            System.currentTimeMillis() - start);


        return ObservableTimer.time(Observable.just(refreshResults), refreshTimer);
    }



    public String[] getIndexes() {
        Set<String> indexSet = new HashSet<>();
        List<String> reads =  Arrays.asList(getIndexes(AliasType.Read));
        List<String> writes = Arrays.asList(getIndexes(AliasType.Write));
        indexSet.addAll(reads);
        indexSet.addAll(writes);
        return indexSet.toArray(new String[0]);
    }

    @Override
    public EntityIndexBatch createBatch() {
        EntityIndexBatch batch =
            new EsEntityIndexBatchImpl(indexLocationStrategy, this );
        return batch;
    }

    public CandidateResults search( final SearchEdge searchEdge, final SearchTypes searchTypes, final String query,
                                    final int limit, final int offset ) {

        IndexValidationUtils.validateSearchEdge(searchEdge);
        Preconditions.checkNotNull(searchTypes, "searchTypes cannot be null");
        Preconditions.checkNotNull( query, "query cannot be null" );
        Preconditions.checkArgument( limit > 0, "limit must be > 0" );


        SearchResponse searchResponse;

        final ParsedQuery parsedQuery = ParsedQueryBuilder.build(query);

        final SearchRequestBuilder srb = searchRequest.getBuilder( searchEdge, searchTypes, parsedQuery, limit, offset )
            .setTimeout( TimeValue.timeValueMillis( queryTimeout ) );

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Searching index (read alias): {}\n  nodeId: {}, edgeType: {},  \n type: {}\n   query: {} ",
                this.alias.getReadAlias(), searchEdge.getNodeId(), searchEdge.getEdgeName(),
                searchTypes.getTypeNames( applicationScope ), srb );
        }

         //Added For Graphite Metrics
        final Timer.Context timerContext = searchTimer.time();

        try {

            searchResponse = srb.execute().actionGet();
        }
        catch ( Throwable t ) {
            logger.error( "Unable to communicate with Elasticsearch", t );
            failureMonitor.fail( "Unable to execute batch", t );
            throw t;
        }
        finally{
            timerContext.stop();
        }

        failureMonitor.success();

        return parseResults( searchResponse, parsedQuery, limit, offset);
    }


    @Override
    public CandidateResults getAllEdgeDocuments( final IndexEdge edge, final Id entityId ) {
        /**
         * Take a list of IndexEdge, with an entityId
         and query Es directly for matches

         */
        IndexValidationUtils.validateSearchEdge(edge);
        Preconditions.checkNotNull( entityId, "entityId cannot be null" );

        SearchResponse searchResponse;

        List<CandidateResult> candidates = new ArrayList<>();

        final ParsedQuery parsedQuery = ParsedQueryBuilder.build( "select *" );

        final SearchRequestBuilder srb = searchRequestBuilderStrategyV2.getBuilder();

        //I can't just search on the entity Id.

        FilterBuilder entityEdgeFilter = FilterBuilders.termFilter(IndexingUtils.EDGE_NODE_ID_FIELDNAME,
            IndexingUtils.nodeId(edge.getNodeId()));

        srb.setPostFilter(entityEdgeFilter);

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Searching for marked versions in index (read alias): {}\n  nodeId: {},\n   query: {} ",
                this.alias.getReadAlias(),entityId, srb );
        }

        try {
            //Added For Graphite Metrics

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
        ValidationUtils.verifyVersion(markedVersion);

        SearchResponse searchResponse;

        List<CandidateResult> candidates = new ArrayList<>();

        final ParsedQuery parsedQuery = ParsedQueryBuilder.build( "select *" );

        final SearchRequestBuilder srb = searchRequestBuilderStrategyV2.getBuilder();

        FilterBuilder entityIdFilter = FilterBuilders.termFilter(IndexingUtils.ENTITY_ID_FIELDNAME,
            IndexingUtils.entityId(entityId));

        FilterBuilder entityVersionFilter = FilterBuilders.rangeFilter( IndexingUtils.ENTITY_VERSION_FIELDNAME ).lte(markedVersion);

        FilterBuilder andFilter = FilterBuilders.andFilter(entityIdFilter, entityVersionFilter);

        srb.setPostFilter(andFilter);



        if ( logger.isDebugEnabled() ) {
            logger.debug( "Searching for marked versions in index (read alias): {}\n  nodeId: {},\n   query: {} ",
                this.alias.getReadAlias(),entityId, srb );
        }

        try {
            //Added For Graphite Metrics

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
        String idString = applicationId(applicationScope.getApplication());
        final TermQueryBuilder tqb = QueryBuilders.termQuery(APPLICATION_ID_FIELDNAME, idString);
        final String[] indexes = getIndexes();
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
        } ).doOnError( t -> logger.error( "Failed on delete application", t ) );
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

    /**
     * Do the retry operation
     */
    private void doInRetry( final RetryOperation operation ) {
        for ( int i = 0; i < MAX_WAITS; i++ ) {

            try {
                operation.doOp();
            }
            catch ( Exception e ) {
                logger.error( "Unable to execute operation, retrying", e );
                try {
                    Thread.sleep( WAIT_TIME );
                } catch ( InterruptedException ie ) {
                    //swallow it
                }
            }



        }
    }


    /**
     * Check health of cluster.
     */
    @Override
    public Health getClusterHealth() {

        try {
            ClusterHealthResponse chr = esProvider.getClient().admin()
                    .cluster().health(new ClusterHealthRequest()).get();
            return Health.valueOf( chr.getStatus().name() );
        }
        catch ( Exception ex ) {
            logger.error( "Error connecting to ElasticSearch", ex );
        }

        // this is bad, red alert!
        return Health.RED;
    }


    /**
     * Check health of this specific index.
     */
    @Override
    public Health getIndexHealth() {

        try {
            String[] indexNames = this.getIndexes();
           final ActionFuture<ClusterHealthResponse> future =  esProvider.getClient().admin().cluster().health(
               new ClusterHealthRequest( indexNames  ) );

            //only wait 2 seconds max
            ClusterHealthResponse chr = future.actionGet(2000);
            return Health.valueOf( chr.getStatus().name() );
        }
        catch ( Exception ex ) {
            logger.error( "Error connecting to ElasticSearch", ex );
        }

        // this is bad, red alert!
        return Health.RED;
    }


    @Override
    public long getEntitySize(final SearchEdge edge){
        //"term":{"edgeName":"zzzcollzzz|roles"}
        SearchRequestBuilder builder = searchRequestBuilderStrategyV2.getBuilder();
        builder.setQuery(new TermQueryBuilder("edgeSearch",IndexingUtils.createContextName(applicationScope,edge)));
        return  getEntitySizeAggregation(builder);
    }

    private long getEntitySizeAggregation( final SearchRequestBuilder builder ) {
        final String key = "entitySize";
        SumBuilder sumBuilder = new SumBuilder(key);
        sumBuilder.field("entitySize");
        builder.addAggregation(sumBuilder);

        Observable<Number> o = Observable.from(builder.execute())
            .map(response -> {
                Sum aggregation = (Sum) response.getAggregations().get(key);
                if(aggregation == null){
                    return -1;
                }else{
                    return aggregation.getValue();
                }
            });
        Number val =   ObservableTimer.time(o,aggregationTimer).toBlocking().lastOrDefault(-1);
        return val.longValue();
    }

    @Override
    public int getImplementationVersion() {
        return IndexDataVersions.SINGLE_INDEX.getVersion();
    }



    /**
     * Interface for operations.
     */
    private interface RetryOperation {

        /**
         * Return true if done, false if there should be a retry.
         */
        boolean doOp();
    }




}
