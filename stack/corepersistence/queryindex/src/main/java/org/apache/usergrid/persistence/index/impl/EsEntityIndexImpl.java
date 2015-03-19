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
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.usergrid.persistence.core.future.BetterFuture;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.util.Health;
import org.apache.usergrid.persistence.index.*;
import org.apache.usergrid.persistence.index.exceptions.IndexException;

import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import org.elasticsearch.action.ActionFuture;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.tasks.PendingClusterTasksRequest;
import org.elasticsearch.action.admin.cluster.tasks.PendingClusterTasksResponse;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesResponse;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequestBuilder;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;

import org.elasticsearch.client.AdminClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.*;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.indices.IndexMissingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;


/**
 * Implements index using ElasticSearch Java API.
 */
@Singleton
public class EsEntityIndexImpl implements AliasedEntityIndex {

    private static final Logger logger = LoggerFactory.getLogger( EsEntityIndexImpl.class );

    public static final String DEFAULT_TYPE = "_default_";

    private final IndexIdentifier.IndexAlias alias;
    private final IndexBufferProducer indexBatchBufferProducer;
    private final IndexFig indexFig;
    private final Timer addTimer;
    private final Timer updateAliasTimer;


    /**
     * We purposefully make this per instance. Some indexes may work, while others may fail
     */


    private final EsProvider esProvider;
    private final IndexFig config;

    //number of times to wait for the index to refresh properly.
    private static final int MAX_WAITS = 10;
    //number of milliseconds to try again before sleeping
    private static final int WAIT_TIME = 250;

    private static final String VERIFY_TYPE = "verification";

    private static final ImmutableMap<String, Object> DEFAULT_PAYLOAD =
            ImmutableMap.<String, Object>builder().put( "field", "test" ).put(IndexingUtils.ENTITYID_ID_FIELDNAME, UUIDGenerator.newTimeUUID().toString()).build();

    private static final MatchAllQueryBuilder MATCH_ALL_QUERY_BUILDER = QueryBuilders.matchAllQuery();
    private final IndexIdentifier indexIdentifier;

    private EsIndexCache aliasCache;
    private Timer mappingTimer;
    private Timer refreshTimer;


//    private final Timer indexTimer;


    @Inject
    public EsEntityIndexImpl( final IndexFig config,
                              final IndexBufferProducer indexBatchBufferProducer, final EsProvider provider,
                              final EsIndexCache indexCache, final MetricsFactory metricsFactory,
                              final IndexFig indexFig, final IndexIdentifier indexIdentifier ) {
        this.indexBatchBufferProducer = indexBatchBufferProducer;
        this.indexFig = indexFig;
        this.indexIdentifier = indexIdentifier;

        this.esProvider = provider;
        this.config = config;



        this.alias = indexIdentifier.getAlias();
        this.aliasCache = indexCache;
        this.addTimer = metricsFactory
            .getTimer(EsEntityIndexImpl.class, "add.timer");
        this.updateAliasTimer = metricsFactory
            .getTimer(EsEntityIndexImpl.class, "update.alias.timer");
        this.mappingTimer = metricsFactory
            .getTimer(EsEntityIndexImpl.class, "create.mapping.timer");
        this.refreshTimer = metricsFactory
            .getTimer(EsEntityIndexImpl.class, "refresh.timer");

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


                logger.info("Created new Index Name [{}] ACK=[{}]", indexName, cir.isAcknowledged());
            } catch (IndexAlreadyExistsException e) {
                logger.info("Index Name [{}] already exists", indexName);
            }
            /**
             * DO NOT MOVE THIS LINE OF CODE UNLESS YOU REALLY KNOW WHAT YOU'RE DOING!!!!
             */

            //We do NOT want to create an alias if the index already exists, we'll overwrite the indexes that
            //may have been set via other administrative endpoint

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

        final Timer.Context timeRemoveAlias = updateAliasTimer.time();
        try {


            String indexName = indexIdentifier.getIndex(indexSuffix);
            final AdminClient adminClient = esProvider.getClient().admin();

            String[] indexNames = getIndexesFromEs(AliasType.Write);


            final IndicesAliasesRequestBuilder aliasesRequestBuilder = adminClient.indices().prepareAliases();

            //remove the write alias from it's target
            for ( String currentIndex : indexNames ) {
                aliasesRequestBuilder.removeAlias( currentIndex, alias.getWriteAlias() );
                logger.info("Removing existing write Alias Name [{}] from Index [{}]", alias.getWriteAlias(), currentIndex);
            }

            //Added For Graphite Metrics

            // add read alias
            aliasesRequestBuilder.addAlias(  indexName, alias.getReadAlias());
            logger.info("Created new read Alias Name [{}] on Index [{}]", alias.getReadAlias(), indexName);


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

        logger.debug("Testing new index name: read {} write {}", alias.getReadAlias(), alias.getWriteAlias());

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
                        logger.debug( "Not refreshing indexes. none found");
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

        doInRetry(retryOperation);
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
            ClusterHealthResponse chr = future.actionGet(2000);
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
