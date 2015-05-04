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
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.usergrid.persistence.core.future.FutureObservable;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.migration.data.VersionedData;
import org.apache.usergrid.persistence.core.util.Health;
import org.apache.usergrid.persistence.index.*;
import org.apache.usergrid.persistence.index.exceptions.IndexException;
import org.apache.usergrid.persistence.index.migration.IndexDataVersions;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;

import java.io.IOException;
import java.net.URL;
import java.util.*;


/**
 * Implements index using ElasticSearch Java API.
 */
@Singleton
public class EsEntityIndexImpl implements AliasedEntityIndex,VersionedData {

    private static final Logger logger = LoggerFactory.getLogger( EsEntityIndexImpl.class );

    private final IndexAlias alias;
    private final IndexBufferConsumer producer;
    private final IndexFig indexFig;
    private final Timer addTimer;
    private final Timer updateAliasTimer;

    /**
     * We purposefully make this per instance. Some indexes may work, while others may fail
     */
    private final EsProvider esProvider;
    private final IndexRefreshCommand indexRefreshCommand;

    //number of times to wait for the index to refresh properly.
    private static final int MAX_WAITS = 10;
    //number of milliseconds to try again before sleeping
    private static final int WAIT_TIME = 250;

    private static final String VERIFY_TYPE = "entity";

    private static final ImmutableMap<String, Object> DEFAULT_PAYLOAD =
            ImmutableMap.<String, Object>builder().put(IndexingUtils.ENTITY_ID_FIELDNAME, UUIDGenerator.newTimeUUID().toString()).build();


    private final IndexIdentifier indexIdentifier;

    private IndexCache aliasCache;
    private Timer mappingTimer;
    private Meter refreshIndexMeter;

//    private final Timer indexTimer;


    @Inject
    public EsEntityIndexImpl( final EsProvider provider,
                              final IndexCache indexCache,
                              final IndexFig indexFig, final IndexIdentifier indexIdentifier,
                             final IndexBufferConsumer producer, final IndexRefreshCommand indexRefreshCommand,
                              final MetricsFactory metricsFactory) {

        this.indexFig = indexFig;
        this.indexIdentifier = indexIdentifier;

        this.esProvider = provider;
        this.producer = producer;
        this.indexRefreshCommand = indexRefreshCommand;
        this.alias = indexIdentifier.getAlias();
        this.aliasCache = indexCache;
        this.addTimer = metricsFactory
            .getTimer(EsEntityIndexImpl.class, "add.timer");
        this.updateAliasTimer = metricsFactory
            .getTimer(EsEntityIndexImpl.class, "update.alias.timer");
        this.mappingTimer = metricsFactory
            .getTimer(EsEntityIndexImpl.class, "create.mapping.timer");

        this.refreshIndexMeter = metricsFactory.getMeter(EsEntityIndexImpl.class,"refresh.meter");

    }

    @Override
    public void initialize() {
        final int numberOfShards = indexFig.getNumberOfShards();
        final int numberOfReplicas = indexFig.getNumberOfReplicas();

        aliasCache.invalidate(alias);

        if (shouldInitialize()) {
            addIndex( null, numberOfShards, numberOfReplicas, indexFig.getWriteConsistencyLevel() );
        }
    }

    private boolean shouldInitialize() {
        String[] reads = getIndexes(AliasedEntityIndex.AliasType.Read);
        String[] writes = getIndexes(AliasedEntityIndex.AliasType.Write);
        return reads.length==0  || writes.length==0;
    }

    @Override
    public void addIndex(final String indexSuffix,final int numberOfShards, final int numberOfReplicas, final String writeConsistency) {
        try {
            //get index name with suffix attached
            String indexName = indexIdentifier.getIndex(indexSuffix);

            //Create index
            try {
                final AdminClient admin = esProvider.getClient().admin();
                Settings settings = ImmutableSettings.settingsBuilder()
                        .put("index.number_of_shards", numberOfShards)
                        .put("index.number_of_replicas", numberOfReplicas)

                        //dont' allow unmapped queries, and don't allow dynamic mapping
                        .put("index.query.parse.allow_unmapped_fields", false )
                        .put("index.mapper.dynamic", false)
                        .put( "action.write_consistency", writeConsistency )
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

            addAlias(indexSuffix);



            testNewIndex();

        } catch (IndexAlreadyExistsException expected) {
            // this is expected to happen if index already exists, it's a no-op and swallow
        } catch (IOException e) {
            throw new RuntimeException("Unable to initialize index", e);
        }
    }


    @Override
    public void addAlias(final String indexSuffix) {
        Timer.Context timer = updateAliasTimer.time();
        try {
            Boolean isAck;
            String indexName = indexIdentifier.getIndex(indexSuffix);
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
            // add read alias
            aliasesRequestBuilder.addAlias(indexName, alias.getReadAlias());
            //Added For Graphite Metrics
            //add write alias
            aliasesRequestBuilder.addAlias(indexName, alias.getWriteAlias());
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
     * Get our index info from ES, but clear our cache first
     * @param aliasType
     * @return
     */
    public String[] getIndexesFromEs(final AliasType aliasType){
        aliasCache.invalidate(alias);
        return getIndexes(aliasType);
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




    public Observable<IndexRefreshCommand.IndexRefreshCommandInfo> refreshAsync() {

        refreshIndexMeter.mark();
        return indexRefreshCommand.execute(getUniqueIndexes());
    }



    public String[] getUniqueIndexes() {
        Set<String> indexSet = new HashSet<>();
        List<String> reads =  Arrays.asList(getIndexes(AliasType.Read));
        List<String> writes = Arrays.asList(getIndexes(AliasType.Write));
        indexSet.addAll(reads);
        indexSet.addAll(writes);
        return indexSet.toArray(new String[0]);
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
