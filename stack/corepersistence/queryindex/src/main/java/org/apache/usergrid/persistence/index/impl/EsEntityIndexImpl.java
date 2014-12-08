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


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.usergrid.persistence.index.*;
import org.apache.usergrid.persistence.index.utils.StringUtils;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.tasks.PendingClusterTasksRequest;
import org.elasticsearch.action.admin.cluster.tasks.PendingClusterTasksResponse;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.Health;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.index.exceptions.IndexException;
import org.apache.usergrid.persistence.index.query.CandidateResult;
import org.apache.usergrid.persistence.index.query.CandidateResults;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import static org.apache.usergrid.persistence.index.impl.IndexingUtils.BOOLEAN_PREFIX;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.NUMBER_PREFIX;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.SPLITTER;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.STRING_PREFIX;


/**
 * Implements index using ElasticSearch Java API.
 */
public class EsEntityIndexImpl implements EntityIndex {

    private static final Logger logger = LoggerFactory.getLogger( EsEntityIndexImpl.class );

    private static final AtomicBoolean mappingsCreated = new AtomicBoolean( false );

    private final IndexIdentifier.IndexAlias alias;
    private final IndexIdentifier indexIdentifier;

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
            ImmutableMap.<String, Object>of( "field", "test" );

    private static final MatchAllQueryBuilder MATCH_ALL_QUERY_BUILDER = QueryBuilders.matchAllQuery();


    @Inject
    public EsEntityIndexImpl( @Assisted final ApplicationScope appScope, final IndexFig config, final EsProvider provider ) {
        ValidationUtils.validateApplicationScope( appScope );
        this.applicationScope = appScope;
        this.esProvider = provider;
        this.config = config;
        this.cursorTimeout = config.getQueryCursorTimeout();
        this.indexIdentifier = IndexingUtils.createIndexIdentifier(config, appScope);
        this.alias = indexIdentifier.getAlias();
        this.failureMonitor = new FailureMonitorImpl( config, provider );
    }

    @Override
    public void initializeIndex() {
        final int numberOfShards = config.getNumberOfShards();
        final int numberOfReplicas = config.getNumberOfReplicas();
        addIndex(null, numberOfShards, numberOfReplicas);
    }

    @Override
    public void addIndex(final String indexSuffix,final int numberOfShards, final int numberOfReplicas) {
        String normalizedSuffix =  StringUtils.isNotEmpty(indexSuffix) ? indexSuffix : null;
        try {

            if (!mappingsCreated.getAndSet(true)) {
                createMappings();
            }

            //get index name with suffix attached
            String indexName = indexIdentifier.getIndex(normalizedSuffix);

            //Create index
            try {
                final AdminClient admin = esProvider.getClient().admin();
                Settings settings = ImmutableSettings.settingsBuilder().put("index.number_of_shards", numberOfShards)
                        .put("index.number_of_replicas", numberOfReplicas).build();
                final CreateIndexResponse cir = admin.indices().prepareCreate(indexName).setSettings(settings).execute().actionGet();
                logger.info("Created new Index Name [{}] ACK=[{}]", indexName, cir.isAcknowledged());
            } catch (IndexAlreadyExistsException e) {
                logger.info("Index Name [{}] already exists", indexName);
            }

            addAlias(normalizedSuffix);

            testNewIndex();
        } catch (IndexAlreadyExistsException expected) {
            // this is expected to happen if index already exists, it's a no-op and swallow
        } catch (IOException e) {
            throw new RuntimeException("Unable to initialize index", e);
        }
    }


    @Override
    public void addAlias(final String indexSuffix) {
        try {
            Boolean isAck;
            String indexName = indexIdentifier.getIndex(indexSuffix);
            final AdminClient adminClient = esProvider.getClient().admin();
            String[] indexNames = getIndexes(alias.getWriteAlias());

            for(String currentIndex : indexNames){
                isAck = adminClient.indices().prepareAliases().removeAlias(currentIndex,alias.getWriteAlias()).execute().actionGet().isAcknowledged();
                logger.info("Removed Index Name [{}] from Alias=[{}] ACK=[{}]",currentIndex, alias, isAck);

            }
            //add read alias
            isAck = adminClient.indices().prepareAliases().addAlias(indexName, alias.getReadAlias()).execute().actionGet().isAcknowledged();
            logger.info("Created new read Alias Name [{}] ACK=[{}]", alias, isAck);
            //add write alias
            isAck = adminClient.indices().prepareAliases().addAlias(indexName, alias.getWriteAlias()).execute().actionGet().isAcknowledged();
            logger.info("Created new write Alias Name [{}] ACK=[{}]", alias, isAck);

        } catch (Exception e) {
            logger.warn("Failed to create alias ", e);
        }
    }

    @Override
    public String[] getIndexes(final AliasType aliasType) {
        final String aliasName = aliasType == AliasType.Read ? alias.getReadAlias() : alias.getWriteAlias();
        return getIndexes(aliasName);
    }

    /**
     * get indexes for alias
     * @param aliasName
     * @return
     */
    private String[] getIndexes(final String aliasName){
        final AdminClient adminClient = esProvider.getClient().admin();
        //remove write alias, can only have one
        ImmutableOpenMap<String,List<AliasMetaData>> aliasMap = adminClient.indices().getAliases(new GetAliasesRequest(aliasName)).actionGet().getAliases();
        return aliasMap.keys().toArray(String.class);
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

        logger.info( "Refreshing Created new Index Name [{}]", alias);

        final RetryOperation retryOperation = new RetryOperation() {
            @Override
            public boolean doOp() {
                final String tempId = UUIDGenerator.newTimeUUID().toString();

                esProvider.getClient().prepareIndex( alias.getWriteAlias(), VERIFY_TYPE, tempId ).setSource( DEFAULT_PAYLOAD )
                          .get();

                logger.info( "Successfully created new document with docId {} in index {} and type {}", tempId,
                        alias, VERIFY_TYPE );

                // delete all types, this way if we miss one it will get cleaned up
                esProvider.getClient().prepareDeleteByQuery( alias.getWriteAlias() ).setTypes(VERIFY_TYPE)
                          .setQuery( MATCH_ALL_QUERY_BUILDER ).get();

                logger.info( "Successfully deleted all documents in index {} and type {}", alias, VERIFY_TYPE );


                return true;
            }
        };

        doInRetry( retryOperation );
    }


    /**
     * Setup ElasticSearch type mappings as a template that applies to all new indexes. 
     * Applies to all indexes that* start with our prefix.
     */
    private void createMappings() throws IOException {

        XContentBuilder xcb = IndexingUtils.createDoubleStringIndexMapping(
                XContentFactory.jsonBuilder(), "_default_");

        PutIndexTemplateResponse pitr = esProvider.getClient().admin().indices()
                .preparePutTemplate("usergrid_template")
                // set mapping as the default for all types
                .setTemplate(config.getIndexPrefix() + "*").addMapping( "_default_", xcb )
                .execute().actionGet();

        if(!pitr.isAcknowledged()){
            throw new IndexException( "Unable to create default mappings" );
        }
    }


    @Override
    public EntityIndexBatch createBatch() {
        return new EsEntityIndexBatchImpl( 
                applicationScope, esProvider.getClient(), config, 1000, failureMonitor, this );
    }


    @Override
    public CandidateResults search( final IndexScope indexScope, final SearchTypes searchTypes, 
            final Query query ) {

        final String context = IndexingUtils.createContextName(indexScope);
        final String[] entityTypes = searchTypes.getTypeNames();

        QueryBuilder qb = query.createQueryBuilder( context );


        SearchResponse searchResponse;

        if ( query.getCursor() == null ) {
            SearchRequestBuilder srb = esProvider.getClient().prepareSearch( alias.getReadAlias() ).setTypes(entityTypes)
                                                 .setScroll(cursorTimeout + "m").setQuery(qb);



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
                logger.debug( "Searching index {}\n  scope{} \n type {}\n   query {} ", new Object[] {
                        this.alias, context, entityTypes, srb
                } );

            }


            try {
                searchResponse = srb.execute().actionGet();
            }
            catch ( Throwable t ) {
                logger.error( "Unable to communicate with elasticsearch", t );
                failureMonitor.fail( "Unable to execute batch", t );
                throw t;
            }


            failureMonitor.success();
        }
        else {
            String scrollId = query.getCursor();
            if ( scrollId.startsWith( "\"" ) ) {
                scrollId = scrollId.substring( 1 );
            }
            if ( scrollId.endsWith( "\"" ) ) {
                scrollId = scrollId.substring( 0, scrollId.length() - 1 );
            }
            logger.debug( "Executing query with cursor: {} ", scrollId );

            SearchScrollRequestBuilder ssrb = esProvider.getClient()
                    .prepareSearchScroll(scrollId).setScroll( cursorTimeout + "m" );

            try {
                searchResponse = ssrb.execute().actionGet();
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

        logger.debug( "   Hit count: {} Total hits: {}", length, searchHits.getTotalHits() );

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
            candidateResults.setCursor( searchResponse.getScrollId() );
            logger.debug("   Cursor = " + searchResponse.getScrollId());
        }

        return candidateResults;
    }


    public void refresh() {


        logger.info( "Refreshing Created new Index Name [{}]", alias);

        final RetryOperation retryOperation = new RetryOperation() {
            @Override
            public boolean doOp() {
                try {
                    esProvider.getClient().admin().indices().prepareRefresh( alias.getReadAlias() ).execute().actionGet();
                    logger.debug( "Refreshed index: " + alias);

                    return true;
                }
                catch ( IndexMissingException e ) {
                    logger.error( "Unable to refresh index after create. Waiting before sleeping.", e );
                    throw e;
                }
            }
        };

        doInRetry( retryOperation );

        logger.debug( "Refreshed index: " + alias);
    }


    @Override
    public int getPendingTasks() {

        final PendingClusterTasksResponse tasksResponse = esProvider.getClient().admin()
                .cluster().pendingClusterTasks(new PendingClusterTasksRequest()).actionGet();

        return tasksResponse.pendingTasks().size();
    }


    @Override
    public CandidateResults getEntityVersions( final IndexScope scope, final Id id ) {

        //since we don't have paging inputs, there's no point in executing a query for paging.

        final String context = IndexingUtils.createContextName(scope);
        final SearchTypes searchTypes = SearchTypes.fromTypes( id.getType() );

        final QueryBuilder queryBuilder = QueryBuilders.termQuery( IndexingUtils.ENTITY_CONTEXT_FIELDNAME, context );


        final SearchRequestBuilder srb =
                esProvider.getClient().prepareSearch( alias.getReadAlias() ).setTypes(searchTypes.getTypeNames())
                          .setScroll(cursorTimeout + "m").setQuery(queryBuilder);



        final SearchResponse searchResponse;
        try {
            searchResponse = srb.execute().actionGet();
        }
        catch ( Throwable t ) {
            logger.error( "Unable to communicate with elasticsearch" );
            failureMonitor.fail( "Unable to execute batch", t );
            throw t;
        }


        failureMonitor.success();

        return parseResults(searchResponse, new Query());
    }


    /**
     * For testing only.
     */
    public void deleteIndex() {
        AdminClient adminClient = esProvider.getClient().admin();
        DeleteIndexResponse response = adminClient.indices().prepareDelete( indexIdentifier.getIndex(null) ).get();
        if ( response.isAcknowledged() ) {
            logger.info( "Deleted index: " + alias);
        }
        else {
            logger.info( "Failed to delete index " + alias);
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
            ClusterHealthResponse chr = esProvider.getClient().admin().cluster()
                                                  .health(new ClusterHealthRequest(new String[]{indexIdentifier.getIndex(null)}))
                                                  .get();
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
