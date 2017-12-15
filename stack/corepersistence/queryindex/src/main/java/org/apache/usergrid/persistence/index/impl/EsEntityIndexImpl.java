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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
import org.apache.usergrid.persistence.index.*;
import org.apache.usergrid.persistence.index.ElasticSearchQueryBuilder.SearchRequestBuilderStrategyV2;
import org.apache.usergrid.persistence.index.exceptions.IndexException;
import org.apache.usergrid.persistence.index.exceptions.QueryAnalyzerException;
import org.apache.usergrid.persistence.index.exceptions.QueryAnalyzerEnforcementException;
import org.apache.usergrid.persistence.index.exceptions.QueryReturnException;
import org.apache.usergrid.persistence.index.migration.IndexDataVersions;
import org.apache.usergrid.persistence.index.query.ParsedQuery;
import org.apache.usergrid.persistence.index.query.ParsedQueryBuilder;
import org.apache.usergrid.persistence.index.query.SortPredicate;
import org.apache.usergrid.persistence.index.query.tree.QueryVisitor;
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
import org.elasticsearch.action.admin.indices.stats.CommonStats;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.deletebyquery.IndexDeleteByQueryResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;
import org.elasticsearch.search.aggregations.metrics.sum.SumBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

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


    private Cache<String, Long> sizeCache =
        CacheBuilder.newBuilder().maximumSize( 1000 ).expireAfterWrite(5, TimeUnit.MINUTES).build();


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

        if (logger.isTraceEnabled()) {
            logger.trace("Testing new index name: read {} write {}", alias.getReadAlias(), alias.getWriteAlias());
        }

        final RetryOperation retryOperation = () -> {
            final String tempId = UUIDGenerator.newTimeUUID().toString();

            esProvider.getClient().prepareIndex( alias.getWriteAlias(), VERIFY_TYPE, tempId )
                .setSource(DEFAULT_PAYLOAD).get();

            if (logger.isTraceEnabled()) {
                logger.trace("Successfully created new document with docId {} in index read {} write {} and type {}",
                    tempId, alias.getReadAlias(), alias.getWriteAlias(), VERIFY_TYPE);
            }

            // delete all types, this way if we miss one it will get cleaned up
            esProvider.getClient().prepareDelete( alias.getWriteAlias(), VERIFY_TYPE, tempId).get();

            if (logger.isTraceEnabled()) {
                logger.trace("Successfully deleted  documents in read {} write {} and type {} with id {}",
                    alias.getReadAlias(), alias.getWriteAlias(), VERIFY_TYPE, tempId);
            }

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
            if (logger.isTraceEnabled()) {
                logger.trace("Not refreshing indexes. none found");
            }
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
        if (logger.isTraceEnabled()) {
            logger.trace("Refreshed indexes: {},success:{} failed:{} ", StringUtils.join(indexes, ", "),
                successfulShards, failedShards);
        }

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
                                    final int limit, final int offset, final boolean analyzeOnly ) {
        return search(searchEdge, searchTypes, query, limit, offset, new HashMap<>(0), analyzeOnly);
    }

    public CandidateResults search( final SearchEdge searchEdge, final SearchTypes searchTypes, final String query,
                                    final int limit, final int offset, final Map<String, Class> fieldsWithType,
                                    final boolean analyzeOnly ) {
        return search(searchEdge, searchTypes, query, limit, offset, fieldsWithType, analyzeOnly, false);
    }

    public CandidateResults search( final SearchEdge searchEdge, final SearchTypes searchTypes, final String query,
                                    final int limit, final int offset, final Map<String, Class> fieldsWithType,
                                    final boolean analyzeOnly, final boolean returnQuery ) {

        IndexValidationUtils.validateSearchEdge(searchEdge);
        Preconditions.checkNotNull(searchTypes, "searchTypes cannot be null");
        Preconditions.checkNotNull( query, "query cannot be null" );
        Preconditions.checkArgument( limit > 0, "limit must be > 0" );


        SearchResponse searchResponse;

        final ParsedQuery parsedQuery = ParsedQueryBuilder.build(query);

        if ( parsedQuery == null ){
            throw new IllegalArgumentException("a null query string cannot be parsed");
        }

        final QueryVisitor visitor = visitParsedQuery(parsedQuery);

        boolean hasGeoSortPredicates = false;

        for (SortPredicate sortPredicate : parsedQuery.getSortPredicates() ){
            hasGeoSortPredicates = visitor.getGeoSorts().contains(sortPredicate.getPropertyName());
        }

        final String cacheKey = applicationScope.getApplication().getUuid().toString()+"_"+searchEdge.getEdgeName();
        final Object totalEdgeSizeFromCache = sizeCache.getIfPresent(cacheKey);
        long totalEdgeSizeInBytes;
        if (totalEdgeSizeFromCache == null){
            totalEdgeSizeInBytes = getTotalEntitySizeInBytes(searchEdge);
            sizeCache.put(cacheKey, totalEdgeSizeInBytes);
        }else{
            totalEdgeSizeInBytes = (long) totalEdgeSizeFromCache;
        }

        final Object totalIndexSizeFromCache = sizeCache.getIfPresent(indexLocationStrategy.getIndexRootName());
        long totalIndexSizeInBytes;
        if (totalIndexSizeFromCache == null){
            totalIndexSizeInBytes = getIndexSize();
            sizeCache.put(indexLocationStrategy.getIndexRootName(), totalIndexSizeInBytes);
        }else{
            totalIndexSizeInBytes = (long) totalIndexSizeFromCache;
        }

        List<Map<String, Object>> violations = QueryAnalyzer.analyze(parsedQuery, totalEdgeSizeInBytes, totalIndexSizeInBytes, indexFig);
        if(indexFig.enforceQueryBreaker() && violations.size() > 0){
            throw new QueryAnalyzerEnforcementException(violations, parsedQuery.getOriginalQuery());
        }else if (violations.size() > 0){
            logger.warn( QueryAnalyzer.violationsAsString(violations, parsedQuery.getOriginalQuery()) );
        }

        if(analyzeOnly){
            throw new QueryAnalyzerException(violations, parsedQuery.getOriginalQuery(), applicationScope.getApplication().getUuid());
        }

        final SearchRequestBuilder srb = searchRequest
            .getBuilder( searchEdge, searchTypes, visitor, limit, offset, parsedQuery.getSortPredicates(), fieldsWithType )
            .setTimeout(TimeValue.timeValueMillis(queryTimeout));

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Searching index (read alias): {}\n  nodeId: {}, edgeType: {},  \n type: {}\n   query: {} ",
                this.alias.getReadAlias(), searchEdge.getNodeId(), searchEdge.getEdgeName(),
                searchTypes.getTypeNames( applicationScope ), srb );
        }

        if (returnQuery) {
            throw new QueryReturnException(parsedQuery.getOriginalQuery(), srb.toString(), applicationScope.getApplication().getUuid());
        }

        //Added For Graphite Metrics
        final Timer.Context timerContext = searchTimer.time();

        try {

            searchResponse = srb.execute().actionGet();
        }
        catch ( Throwable t ) {
            logger.error( "Unable to communicate with Elasticsearch: {}", t.getMessage() );
            failureMonitor.fail( "Unable to execute batch", t );
            throw t;
        }
        finally{
            timerContext.stop();
        }

        failureMonitor.success();

        return parseResults( searchResponse, parsedQuery, limit, offset, hasGeoSortPredicates);
    }


    @Override
    public CandidateResults getAllEdgeDocuments( final IndexEdge edge, final Id entityId ) {
        /**
         * Take a list of IndexEdge, with an entityId
         and query Es directly for matches

         */
        IndexValidationUtils.validateSearchEdge(edge);
        Preconditions.checkNotNull(entityId, "entityId cannot be null");

        SearchResponse searchResponse;
        List<CandidateResult> candidates = new ArrayList<>();

        // never let this fetch more than 100 to save memory
        final int searchLimit = Math.min(100, indexFig.getVersionQueryLimit());

        final QueryBuilder nodeIdQuery = QueryBuilders
            .termQuery(IndexingUtils.EDGE_NODE_ID_FIELDNAME, IndexingUtils.nodeId(edge.getNodeId()));

        final QueryBuilder entityIdQuery = QueryBuilders
            .termQuery(IndexingUtils.ENTITY_ID_FIELDNAME, IndexingUtils.entityId(entityId));

        final SearchRequestBuilder srb = searchRequestBuilderStrategyV2.getBuilder()
            .addSort(IndexingUtils.EDGE_TIMESTAMP_FIELDNAME, SortOrder.ASC);


        if ( logger.isDebugEnabled() ) {
            logger.debug( "Searching for edges in (read alias): {}\n  nodeId: {},\n   query: {} ",
                this.alias.getReadAlias(),entityId, srb );
        }

        try {

            long queryTimestamp = 0L;


            QueryBuilder timestampQuery =  QueryBuilders
                .rangeQuery(IndexingUtils.EDGE_TIMESTAMP_FIELDNAME)
                .gte(queryTimestamp);

            QueryBuilder finalQuery = QueryBuilders.constantScoreQuery(
                QueryBuilders
                    .boolQuery()
                    .must(entityIdQuery)
                    .must(nodeIdQuery)
                    .must(timestampQuery)
            );

            searchResponse = srb
                .setQuery(finalQuery)
                .setSize(searchLimit)
                .execute()
                .actionGet();

            candidates = aggregateScrollResults(candidates, searchResponse, null);

        }
        catch ( Throwable t ) {
            logger.error( "Unable to communicate with Elasticsearch", t.getMessage() );
            failureMonitor.fail( "Unable to execute batch", t );
            throw t;
        }
        failureMonitor.success();

        return new CandidateResults( candidates, Collections.EMPTY_SET);
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
                    logger.error( "Failed on delete index", e.getMessage() );
                }
            } );
            return Observable.from( response );
        } ).doOnError( t -> logger.error( "Failed on delete application", t.getMessage() ) );
    }


    /**
     * Validate the response doesn't contain errors, if it does, fail fast at the first error we encounter
     */
    private void checkDeleteByQueryResponse( final QueryBuilder query, final DeleteByQueryResponse response ) {

        for ( IndexDeleteByQueryResponse indexDeleteByQueryResponse : response ) {
            final ShardOperationFailedException[] failures = indexDeleteByQueryResponse.getFailures();

            for ( ShardOperationFailedException failedException : failures ) {
                logger.error("Unable to delete by query {}. Failed with code {} and reason {} on shard {} in index {}",
                    query.toString(),
                    failedException.status().getStatus(), failedException.reason(),
                    failedException.shardId(), failedException.index() );
            }
        }
    }


    /**
     * Parse the results and return the canddiate results
     */
    private CandidateResults parseResults( final SearchResponse searchResponse, final ParsedQuery query,
                                           final int limit, final int from, boolean hasGeoSortPredicates ) {

        final SearchHits searchHits = searchResponse.getHits();
        final SearchHit[] hits = searchHits.getHits();

        if (logger.isTraceEnabled()) {
            logger.trace("   Hit count: {} Total hits: {}", hits.length, searchHits.getTotalHits());
        }

        List<CandidateResult> candidates = new ArrayList<>( hits.length );

        for ( SearchHit hit : hits ) {
            CandidateResult candidateResult;

            candidateResult =  parseIndexDocId( hit, hasGeoSortPredicates );
            candidates.add( candidateResult );
        }

        final CandidateResults candidateResults = new CandidateResults( candidates, query.getSelectFieldMappings());

        // >= seems odd.  However if we get an overflow, we need to account for it.
        if (  hits.length >= limit ) {

            candidateResults.initializeOffset( from + limit );

        }

        return candidateResults;
    }

    private List<CandidateResult> aggregateScrollResults(List<CandidateResult> candidates,
                                                         final SearchResponse searchResponse, final UUID markedVersion){

        final SearchHits searchHits = searchResponse.getHits();
        final SearchHit[] hits = searchHits.getHits();

        for ( SearchHit hit : hits ) {

            final CandidateResult candidateResult = parseIndexDocId( hit );

            // if comparing against the latestVersion, make sure we only add the candidateResult if it's
            // older than or equal to the latest marked version
            if (markedVersion != null) {

                if(candidateResult.getVersion().timestamp() <= markedVersion.timestamp()){

                    if(logger.isTraceEnabled()){
                        logger.trace("Candidate version {} is <= provided entity version {} for entityId {}",
                            candidateResult.getVersion(),
                            markedVersion,
                            candidateResult.getId()
                        );
                    }

                    candidates.add(candidateResult);

                }else{
                    if(logger.isTraceEnabled()){
                        logger.trace("Candidate version {} is > provided entity version {} for entityId {}. Not" +
                                "adding to candidate results",
                            candidateResult.getVersion(),
                            markedVersion,
                            candidateResult.getId()
                        );
                    }
                }

            }else{
                candidates.add(candidateResult);
            }
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Aggregated {} out of {} hits ", candidates.size(), searchHits.getTotalHits());
        }

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
                logger.error( "Unable to execute operation, retrying", e.getMessage() );
                try {
                    Thread.sleep( WAIT_TIME );
                } catch ( InterruptedException ie ) {
                    //swallow it
                }
            }



        }
    }


    /**
     * Perform our visit of the query once for efficiency
     */
    private QueryVisitor visitParsedQuery( final ParsedQuery parsedQuery ) {
        QueryVisitor v = new EsQueryVistor();

        if ( parsedQuery.getRootOperand() != null ) {

            try {
                parsedQuery.getRootOperand().visit( v );
            }
            catch ( IndexException ex ) {
                throw new RuntimeException( "Error building ElasticSearch query", ex );
            }
        }

        return v;
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
            ex.printStackTrace();
            logger.error( "Error connecting to ElasticSearch", ex.getMessage() );
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
            logger.error( "Error connecting to ElasticSearch", ex.getMessage() );
        }

        // this is bad, red alert!
        return Health.RED;
    }

    private long getIndexSize(){
        long indexSize = 0L;
        final String indexName = indexLocationStrategy.getIndexInitialName();
        try {
            final IndicesStatsResponse statsResponse = esProvider.getClient()
                .admin()
                .indices()
                .prepareStats(indexName)
                .all()
                .execute()
                .actionGet();
            final CommonStats indexStats = statsResponse.getIndex(indexName).getTotal();
            indexSize = indexStats.getStore().getSizeInBytes();
        } catch (IndexMissingException e) {
            // if for some reason the index size does not exist,
            // log an error and we can assume size is 0 as it doesn't exist
            logger.error("Unable to get size for index {} due to IndexMissingException for app {}",
                indexName, indexLocationStrategy.getApplicationScope().getApplication().getUuid());
        }
        return indexSize;
    }

    @Override
    public long getTotalEntitySizeInBytes(final SearchEdge edge){
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
