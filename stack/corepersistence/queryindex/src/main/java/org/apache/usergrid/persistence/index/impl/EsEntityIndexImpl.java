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


import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.usergrid.persistence.core.future.BetterFuture;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.Health;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.index.*;
import org.apache.usergrid.persistence.index.exceptions.IndexException;
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

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.tasks.PendingClusterTasksRequest;
import org.elasticsearch.action.admin.cluster.tasks.PendingClusterTasksResponse;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequestBuilder;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.*;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.usergrid.persistence.index.impl.IndexingUtils.*;


/**
 * Implements index using ElasticSearch Java API.
 */
public class EsEntityIndexImpl implements AliasedEntityIndex {

    private static final Logger logger = LoggerFactory.getLogger( EsEntityIndexImpl.class );

    private static final AtomicBoolean mappingsCreated = new AtomicBoolean( false );
    public static final String DEFAULT_TYPE = "_default_";

    private final IndexIdentifier.IndexAlias alias;
    private final IndexIdentifier indexIdentifier;
    private final IndexBufferProducer indexBatchBufferProducer;
    private final IndexFig indexFig;
    private final Timer addTimer;
    private final Timer updateAliasTimer;
    private final Timer searchTimer;

    /**
     * We purposefully make this per instance. Some indexes may work, while others may fail
     */
    private FailureMonitor failureMonitor;

    private final ApplicationScope applicationScope;

    private final EsProvider esProvider;

    private final int cursorTimeout;

    private final IndexFig config;



    //number of times to wait for the index to refresh properly.
    private static final int MAX_WAITS = 10;
    //number of milliseconds to try again before sleeping
    private static final int WAIT_TIME = 250;

    private static final String VERIFY_TYPE = "verification";

    private static final ImmutableMap<String, Object> DEFAULT_PAYLOAD =
            ImmutableMap.<String, Object>builder().put( "field", "test" ).put(IndexingUtils.ENTITYID_ID_FIELDNAME, UUIDGenerator.newTimeUUID().toString()).build();

    private static final MatchAllQueryBuilder MATCH_ALL_QUERY_BUILDER = QueryBuilders.matchAllQuery();

    private EsIndexCache aliasCache;
    private Timer mappingTimer;
    private Timer refreshTimer;
    private Timer cursorTimer;
    private Timer getVersionsTimer;

    private final MapManager mapManager;

//    private final Timer indexTimer;


    @Inject
    public EsEntityIndexImpl( @Assisted final ApplicationScope appScope, final IndexFig config,
                              final IndexBufferProducer indexBatchBufferProducer, final EsProvider provider,
                              final EsIndexCache indexCache, final MetricsFactory metricsFactory,
                              final MapManagerFactory mapManagerFactory, final IndexFig indexFig ) {
        this.indexBatchBufferProducer = indexBatchBufferProducer;
        this.indexFig = indexFig;
        ValidationUtils.validateApplicationScope( appScope );
        this.applicationScope = appScope;
        this.esProvider = provider;
        this.config = config;
        this.cursorTimeout = config.getQueryCursorTimeout();
        this.indexIdentifier = IndexingUtils.createIndexIdentifier( config, appScope );
        this.alias = indexIdentifier.getAlias();
        this.failureMonitor = new FailureMonitorImpl( config, provider );
        this.aliasCache = indexCache;
        this.addTimer = metricsFactory
            .getTimer( EsEntityIndexImpl.class, "add.timer" );
        this.updateAliasTimer = metricsFactory
            .getTimer( EsEntityIndexImpl.class, "update.alias.timer" );
        this.mappingTimer = metricsFactory
            .getTimer( EsEntityIndexImpl.class, "create.mapping.timer" );
        this.refreshTimer = metricsFactory
            .getTimer( EsEntityIndexImpl.class, "refresh.timer" );
        this.searchTimer =metricsFactory
            .getTimer( EsEntityIndexImpl.class, "search.timer" );
        this.cursorTimer = metricsFactory
            .getTimer( EsEntityIndexImpl.class, "search.cursor.timer" );
        this.getVersionsTimer =metricsFactory
            .getTimer( EsEntityIndexImpl.class, "get.versions.timer" );


        final MapScope mapScope = new MapScopeImpl( appScope.getApplication(), "cursorcache" );

        mapManager = mapManagerFactory.createMapManager(mapScope);
    }

    @Override
    public void initializeIndex() {
        final int numberOfShards = config.getNumberOfShards();
        final int numberOfReplicas = config.getNumberOfReplicas();
        String[] indexes = getIndexesFromEs(AliasType.Write);
        if(indexes == null || indexes.length==0) {
            addIndex(null, numberOfShards, numberOfReplicas, config.getWriteConsistencyLevel());
        }
    }

    @Override
    public void addIndex(final String indexSuffix,final int numberOfShards, final int numberOfReplicas, final String writeConsistency) {
        String normalizedSuffix =  StringUtils.isNotEmpty(indexSuffix) ? indexSuffix : null;
        try {
            //get index name with suffix attached
            String indexName = indexIdentifier.getIndex(normalizedSuffix);

            //Create index
            try {
                final AdminClient admin = esProvider.getClient().admin();
                Settings settings = ImmutableSettings.settingsBuilder()
                        .put("index.number_of_shards", numberOfShards)
                        .put("index.number_of_replicas", numberOfReplicas)
                        .put("action.write_consistency", writeConsistency )
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

                /**
                 * DO NOT MOVE THIS LINE OF CODE UNLESS YOU REALLY KNOW WHAT YOU'RE DOING!!!!
                 */

                //We do NOT want to create an alias if the index already exists, we'll overwrite the indexes that
                //may have been set via other administrative endpoint

                addAlias(normalizedSuffix);

                testNewIndex();

                logger.info("Created new Index Name [{}] ACK=[{}]", indexName, cir.isAcknowledged());
            } catch (IndexAlreadyExistsException e) {
                logger.info("Index Name [{}] already exists", indexName);
            }



        } catch (IndexAlreadyExistsException expected) {
            // this is expected to happen if index already exists, it's a no-op and swallow
        } catch (IOException e) {
            throw new RuntimeException("Unable to initialize index", e);
        }
    }


    @Override
    public void addAlias(final String indexSuffix) {

        final Timer.Context timeRemoveAlias = updateAliasTimer.time();
        try {


            String indexName = indexIdentifier.getIndex(indexSuffix);
            final AdminClient adminClient = esProvider.getClient().admin();

            String[] indexNames = getIndexesFromEs( AliasType.Write );


            final IndicesAliasesRequestBuilder aliasesRequestBuilder = adminClient.indices().prepareAliases();

            //remove the write alias from it's target
            for ( String currentIndex : indexNames ) {
                aliasesRequestBuilder.removeAlias( currentIndex, alias.getWriteAlias() );
                logger.info("Removing existing write Alias Name [{}] from Index [{}]", alias.getWriteAlias(), currentIndex);
            }

            //Added For Graphite Metrics

            // add read alias
            aliasesRequestBuilder.addAlias(  indexName, alias.getReadAlias());
            logger.info( "Created new read Alias Name [{}] on Index [{}]", alias.getReadAlias(), indexName);


            //add write alias
            aliasesRequestBuilder.addAlias( indexName, alias.getWriteAlias() );

            logger.info("Created new write Alias Name [{}] on Index [{}]", alias.getWriteAlias(), indexName);

            final IndicesAliasesResponse result = aliasesRequestBuilder.execute().actionGet();

            final boolean isAcknowledged = result.isAcknowledged();

            if(!isAcknowledged){
                throw new RuntimeException( "Unable to add aliases to the new index.  Elasticsearch did not acknowledge to the alias change for index '" + indexSuffix + "'");
            }

        }
        finally{
            //invalidate the alias
            aliasCache.invalidate(alias);
            //stop the timer
            timeRemoveAlias.stop();
        }
    }

    @Override
    public String[] getIndexes(final AliasType aliasType) {
        return aliasCache.getIndexes(alias, aliasType);
    }


    /**
     * Get our index info from ES, but clear our cache first
     * @param aliasType
     * @return
     */
    public String[] getIndexesFromEs(final AliasType aliasType){
        aliasCache.invalidate( alias );
        return getIndexes( aliasType );
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

        logger.debug( "Testing new index name: read {} write {}", alias.getReadAlias(), alias.getWriteAlias());

        final RetryOperation retryOperation = new RetryOperation() {
            @Override
            public boolean doOp() {
                final String tempId = UUIDGenerator.newTimeUUID().toString();

                esProvider.getClient().prepareIndex( alias.getWriteAlias(), VERIFY_TYPE, tempId )
                     .setSource(DEFAULT_PAYLOAD).get();

                logger.info( "Successfully created new document with docId {} "
                     + "in index read {} write {} and type {}",
                        tempId, alias.getReadAlias(), alias.getWriteAlias(), VERIFY_TYPE );

                // delete all types, this way if we miss one it will get cleaned up
                esProvider.getClient().prepareDeleteByQuery( alias.getWriteAlias())
                        .setTypes(VERIFY_TYPE)
                        .setQuery(MATCH_ALL_QUERY_BUILDER).get();

                logger.info( "Successfully deleted all documents in index {} read {} write {} and type {}",
                        alias.getReadAlias(), alias.getWriteAlias(), VERIFY_TYPE );

                return true;
            }
        };

        doInRetry( retryOperation );
    }


    /**
     * Setup ElasticSearch type mappings as a template that applies to all new indexes.
     * Applies to all indexes that* start with our prefix.
     */
    private void createMappings(final String indexName) throws IOException {

        XContentBuilder xcb = IndexingUtils.createDoubleStringIndexMapping( XContentFactory.jsonBuilder(),
            DEFAULT_TYPE );


        //Added For Graphite Metrics
        Timer.Context timePutIndex = mappingTimer.time();
        PutMappingResponse  pitr = esProvider.getClient().admin().indices().preparePutMapping( indexName ).setType(
            DEFAULT_TYPE ).setSource( xcb ).execute().actionGet();
        timePutIndex.stop();
        if ( !pitr.isAcknowledged() ) {
            throw new IndexException( "Unable to create default mappings" );
        }
    }


    @Override
    public EntityIndexBatch createBatch() {
        EntityIndexBatch batch = new EsEntityIndexBatchImpl(
                applicationScope, indexBatchBufferProducer, config, this );
        return batch;
    }


    @Override
    public CandidateResults search(final IndexScope indexScope, final SearchTypes searchTypes,
            final Query query ) {

        final String context = IndexingUtils.createContextName( indexScope );
        final String[] entityTypes = searchTypes.getTypeNames();

        QueryBuilder qb = query.createQueryBuilder(context);


        SearchResponse searchResponse;

        if ( query.getCursor() == null ) {
            SearchRequestBuilder srb = esProvider.getClient().prepareSearch( alias.getReadAlias() )
                    .setTypes(entityTypes)
                    .setScroll(cursorTimeout + "m")
                    .setQuery(qb);

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
                final FieldSortBuilder stringSort = SortBuilders.fieldSort( stringFieldName )
                        .order( order ).ignoreUnmapped( true );
                srb.addSort( stringSort );

                logger.debug( "   Sort: {} order by {}", stringFieldName, order.toString() );

                final String numberFieldName = NUMBER_PREFIX + sp.getPropertyName();
                final FieldSortBuilder numberSort = SortBuilders.fieldSort( numberFieldName )
                        .order( order ).ignoreUnmapped( true );
                srb.addSort( numberSort );
                logger.debug( "   Sort: {} order by {}", numberFieldName, order.toString() );

                final String booleanFieldName = BOOLEAN_PREFIX + sp.getPropertyName();
                final FieldSortBuilder booleanSort = SortBuilders.fieldSort( booleanFieldName )
                        .order( order ).ignoreUnmapped( true );
                srb.addSort( booleanSort );
                logger.debug( "   Sort: {} order by {}", booleanFieldName, order.toString() );
            }


            if ( logger.isDebugEnabled() ) {
                logger.debug( "Searching index (read alias): {}\n  scope: {} \n type: {}\n   query: {} ",
                    this.alias.getReadAlias(), context, entityTypes, srb );
            }

            try {
                //Added For Graphite Metrics
                Timer.Context timeSearch = searchTimer.time();
                searchResponse = srb.execute().actionGet(config.getQueryTimeout());
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

            Preconditions.checkArgument(esScrollCursor != null, "Could not find a cursor for the value '{}' ",  esScrollCursor);



            logger.debug( "Executing query with cursor: {} ", esScrollCursor );


            SearchScrollRequestBuilder ssrb = esProvider.getClient()
                    .prepareSearchScroll(esScrollCursor).setScroll( cursorTimeout + "m" );

            try {
                //Added For Graphite Metrics
                Timer.Context timeSearchCursor = cursorTimer.time();
                searchResponse = ssrb.execute().actionGet(config.getQueryTimeout());
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

            Id entityId = new SimpleId( UUID.fromString( id ), type );

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


    public void refresh() {

        BetterFuture future = indexBatchBufferProducer.put(new IndexOperationMessage());
        future.get();
        //loop through all batches and retrieve promises and call get

        final RetryOperation retryOperation = new RetryOperation() {
            @Override
            public boolean doOp() {
                try {
                    String[] indexes = ArrayUtils.addAll(
                        getIndexes(AliasType.Read),
                        getIndexes(AliasType.Write)
                    );

                    if ( indexes.length == 0 ) {
                        logger.debug( "Not refreshing indexes, none found for app {}",
                                applicationScope.getApplication().getUuid() );
                        return true;
                    }
                    //Added For Graphite Metrics
                    Timer.Context timeRefreshIndex = refreshTimer.time();
                    esProvider.getClient().admin().indices().prepareRefresh( indexes ).execute().actionGet();
                    timeRefreshIndex.stop();
                    logger.debug("Refreshed indexes: {}", StringUtils.join(indexes, ", "));
                    return true;
                }
                catch ( IndexMissingException e ) {
                    logger.error( "Unable to refresh index. Waiting before sleeping.", e );
                    throw e;
                }
            }
        };

        doInRetry( retryOperation );
    }


    @Override
    public int getPendingTasks() {

        final PendingClusterTasksResponse tasksResponse = esProvider.getClient().admin()
                .cluster().pendingClusterTasks( new PendingClusterTasksRequest() ).actionGet();

        return tasksResponse.pendingTasks().size();
    }


    @Override
    public CandidateResults getEntityVersions( final IndexScope scope, final Id id ) {

        //since we don't have paging inputs, there's no point in executing a query for paging.

        final String context = IndexingUtils.createContextName(scope);
        final SearchTypes searchTypes = SearchTypes.fromTypes(id.getType());

        final QueryBuilder queryBuilder =
                QueryBuilders.termQuery( IndexingUtils.ENTITY_CONTEXT_FIELDNAME, context );

        final SearchRequestBuilder srb = esProvider.getClient().prepareSearch( alias.getReadAlias() )
                .setTypes(searchTypes.getTypeNames())
                .setScroll(cursorTimeout + "m")
                .setQuery(queryBuilder);

        final SearchResponse searchResponse;
        try {
            //Added For Graphite Metrics
            Timer.Context timeEntityIndex = getVersionsTimer.time();
            searchResponse = srb.execute().actionGet();
            timeEntityIndex.stop();
        }
        catch ( Throwable t ) {
            logger.error( "Unable to communicate with elasticsearch" );
            failureMonitor.fail( "Unable to execute batch", t);
            throw t;
        }


        failureMonitor.success();

        return parseResults(searchResponse, new Query());
    }



    /**
     * Completely delete an index.
     */
    public void deleteIndex() {
        AdminClient adminClient = esProvider.getClient().admin();

        DeleteIndexResponse response = adminClient.indices()
                .prepareDelete( indexIdentifier.getIndex(null) ).get();

        if ( response.isAcknowledged() ) {
            logger.info( "Deleted index: read {} write {}", alias.getReadAlias(), alias.getWriteAlias());
            //invlaidate the alias
            aliasCache.invalidate(alias);
        }
        else {
            logger.info( "Failed to delete index: read {} write {}", alias.getReadAlias(), alias.getWriteAlias());
        }
    }


    /**
     * Do the retry operation
     */
    private void doInRetry( final RetryOperation operation ) {
        for ( int i = 0; i < MAX_WAITS; i++ ) {

            try {
                if ( operation.doOp() ) {
                    return;
                }
            }
            catch ( Exception e ) {
                logger.error( "Unable to execute operation, retrying", e );
            }


            try {
                Thread.sleep( WAIT_TIME );
            }
            catch ( InterruptedException e ) {
                //swallow it
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
           final ActionFuture<ClusterHealthResponse> future =  esProvider.getClient().admin().cluster().health(
               new ClusterHealthRequest( new String[] { indexIdentifier.getIndex( null ) } ) );

            //only wait 2 seconds max
            ClusterHealthResponse chr = future.actionGet( 2000 );
            return Health.valueOf( chr.getStatus().name() );
        }
        catch ( Exception ex ) {
            logger.error( "Error connecting to ElasticSearch", ex );
        }

        // this is bad, red alert!
        return Health.RED;
    }


    /**
     * Interface for operations.
     */
    private static interface RetryOperation {

        /**
         * Return true if done, false if there should be a retry.
         */
        public boolean doOp();
    }
}
