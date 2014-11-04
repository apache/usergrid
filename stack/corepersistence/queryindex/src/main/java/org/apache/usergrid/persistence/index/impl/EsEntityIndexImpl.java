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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
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
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexBatch;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.query.CandidateResult;
import org.apache.usergrid.persistence.index.query.CandidateResults;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.util.HashMap;
import org.apache.usergrid.persistence.core.util.Health;

import static org.apache.usergrid.persistence.index.impl.IndexingUtils.BOOLEAN_PREFIX;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.DOC_ID_SEPARATOR_SPLITTER;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.ENTITYID_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.NUMBER_PREFIX;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.STRING_PREFIX;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;


/**
 * Implements index using ElasticSearch Java API.
 */
public class EsEntityIndexImpl implements EntityIndex {

    private static final Logger logger = LoggerFactory.getLogger( EsEntityIndexImpl.class );

    private static final AtomicBoolean mappingsCreated = new AtomicBoolean( false );

    private final String indexName;

    private final ApplicationScope applicationScope;

    private final Client client;

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
    public EsEntityIndexImpl( @Assisted final ApplicationScope appScope, final IndexFig config,
                              final EsProvider provider ) {

        ValidationUtils.validateApplicationScope( appScope );

        this.applicationScope = appScope;
        this.client = provider.getClient();
        this.config = config;
        this.cursorTimeout = config.getQueryCursorTimeout();
        this.indexName = IndexingUtils.createIndexName( config.getIndexPrefix(), appScope );
    }


    @Override
    public void initializeIndex() {

        try {
            if ( !mappingsCreated.getAndSet( true ) ) {
                createMappings();
            }

            AdminClient admin = client.admin();

            CreateIndexResponse cir = admin.indices()
                .prepareCreate( indexName )
                .setSettings( new HashMap<String, Object>() {{
                    put("index.number_of_shards", config.getNumberOfShards() );
                    put("index.number_of_replicas", config.numberOfReplicas() );
                }} )
                .execute()
                .actionGet();

            logger.info( "Created new Index Name [{}] ACK=[{}]", indexName, cir.isAcknowledged() );

            // create the document, this ensures the index is ready

            // Immediately create a document and remove it to ensure the entire cluster is ready 
            // to receive documents. Occasionally we see errors.  
            // See this post: http://s.apache.org/index-missing-exception

            testNewIndex();
        }
        catch ( IndexAlreadyExistsException expected ) {
            // this is expected to happen if index already exists, it's a no-op and swallow
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Unable to initialize index", e );
        }
    }


    /**
     * Tests writing a document to a new index to ensure it's working correctly. 
     * See this post: http://s.apache.org/index-missing-exception
     */
    private void testNewIndex() {


        logger.info( "Refreshing Created new Index Name [{}]", indexName );

        final RetryOperation retryOperation = new RetryOperation() {
            @Override
            public boolean doOp() {
                final String tempId = UUIDGenerator.newTimeUUID().toString();

                client.prepareIndex( indexName, VERIFY_TYPE, tempId )
                        .setSource( DEFAULT_PAYLOAD ).get();

                logger.info( "Successfully created new document with docId {} in index {} and type {}", 
                        tempId, indexName, VERIFY_TYPE );

                // delete all types, this way if we miss one it will get cleaned up
                client.prepareDeleteByQuery( indexName ).setTypes( VERIFY_TYPE )
                        .setQuery( MATCH_ALL_QUERY_BUILDER ).get();

                logger.info( "Successfully deleted all documents in index {} and type {}", 
                        indexName, VERIFY_TYPE );

                return true;
            }
        };

        doInRetry( retryOperation );
    }


    /**
     * Setup ElasticSearch type mappings as a template that applies to all new indexes. 
     * Applies to all indexes that start with our prefix.
     */
    private void createMappings() throws IOException {

        XContentBuilder xcb = IndexingUtils
                .createDoubleStringIndexMapping( XContentFactory.jsonBuilder(), "_default_" );

        PutIndexTemplateResponse pitr = client.admin().indices()
            .preparePutTemplate( "usergrid_template" )
            .setTemplate( config.getIndexPrefix() + "*" )
            .addMapping( "_default_", xcb ) // set mapping as the default for all types
            .execute().actionGet();
    }


    @Override
    public EntityIndexBatch createBatch() {
        return new EsEntityIndexBatchImpl( applicationScope, client, config, 1000 );
    }


    @Override
    public CandidateResults search( final IndexScope indexScope, final Query query ) {

        final String indexType = IndexingUtils.createCollectionScopeTypeName( indexScope );

        QueryBuilder qb = query.createQueryBuilder();

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Searching index {}\n   type {}\n   query {} limit {}", new Object[] {
                    this.indexName, indexType, qb.toString().replace( "\n", " " ), query.getLimit()
            } );
        }

        SearchResponse searchResponse;
        if ( query.getCursor() == null ) {

            SearchRequestBuilder srb = client.prepareSearch( indexName )
                    .setTypes( indexType ).setScroll( cursorTimeout + "m" ) .setQuery( qb );

            FilterBuilder fb = query.createFilterBuilder();
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

            searchResponse = srb.execute().actionGet();
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

            SearchScrollRequestBuilder ssrb = client.prepareSearchScroll( scrollId )
                    .setScroll( cursorTimeout + "m" );
            searchResponse = ssrb.execute().actionGet();
        }

        SearchHits hits = searchResponse.getHits();
        logger.debug( "   Hit count: {} Total hits: {}", hits.getHits().length, hits.getTotalHits() );

        List<CandidateResult> candidates = new ArrayList<CandidateResult>();

        for ( SearchHit hit : hits.getHits() ) {

            String[] idparts = hit.getId().split( DOC_ID_SEPARATOR_SPLITTER );
            String id = idparts[0];
            String type = idparts[1];
            String version = idparts[2];

            Id entityId = new SimpleId( UUID.fromString( id ), type );

            candidates.add( new CandidateResult( entityId, UUID.fromString( version ) ) );
        }

        CandidateResults candidateResults = new CandidateResults( query, candidates );

        if ( candidates.size() >= query.getLimit() ) {
            candidateResults.setCursor( searchResponse.getScrollId() );
            logger.debug( "   Cursor = " + searchResponse.getScrollId() );
        }

        return candidateResults;
    }


    public void refresh() {


        logger.info( "Refreshing Created new Index Name [{}]", indexName );

        final RetryOperation retryOperation = new RetryOperation() {
            @Override
            public boolean doOp() {
                try {
                    client.admin().indices().prepareRefresh( indexName ).execute().actionGet();
                    logger.debug( "Refreshed index: " + indexName );
                    return true;
                }
                catch ( IndexMissingException e ) {
                    logger.error( "Unable to refresh index after create. Waiting before sleeping.", e);
                    throw e;
                }
            }
        };

        doInRetry( retryOperation );

        logger.debug( "Refreshed index: " + indexName );
    }


    @Override
    public CandidateResults getEntityVersions( final IndexScope scope, final Id id ) {
        Query query = new Query();
        query.addEqualityFilter( ENTITYID_FIELDNAME, id.getUuid().toString() );
        CandidateResults results = search( scope, query );
        return results;
    }


    /**
     * For testing only.
     */
    public void deleteIndex() {
        AdminClient adminClient = client.admin();
        DeleteIndexResponse response = adminClient.indices().prepareDelete( indexName ).get();
        if ( response.isAcknowledged() ) {
            logger.info( "Deleted index: " + indexName );
        }
        else {
            logger.info( "Failed to delete index " + indexName );
        }
    }


    /**
     * Do the retry operation
     * @param operation
     */
    private void doInRetry( final RetryOperation operation ) {
        for ( int i = 0; i < MAX_WAITS; i++ ) {

            try {
                if(operation.doOp()){
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
            ClusterHealthResponse chr = client.admin().cluster()
                .health( new ClusterHealthRequest() ).get();
            return Health.valueOf( chr.getStatus().name() );
        } 
        catch (Exception ex) {
            logger.error("Error connecting to ElasticSearch", ex);
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
            ClusterHealthResponse chr = client.admin().cluster()
                .health( new ClusterHealthRequest( new String[] { indexName } ) ).get();
            return Health.valueOf( chr.getStatus().name() );
        } 
        catch (Exception ex) {
            logger.error("Error connecting to ElasticSearch", ex);
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
