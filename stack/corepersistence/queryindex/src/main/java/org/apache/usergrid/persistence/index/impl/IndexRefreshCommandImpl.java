/*
 *
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
 *
 */
package org.apache.usergrid.persistence.index.impl;


import java.util.Map;
import java.util.UUID;

import org.apache.usergrid.persistence.index.*;
import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.metrics.ObservableTimer;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.util.StringUtils;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.EntityUtils;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.codahale.metrics.Timer;
import com.google.inject.Inject;

import rx.Observable;


/**
 * Classy class class.
 */
public class IndexRefreshCommandImpl implements IndexRefreshCommand {
    private static final Logger logger = LoggerFactory.getLogger( IndexRefreshCommandImpl.class );

    private final IndexCache indexCache;
    private final EsProvider esProvider;
    private final IndexProducer producer;
    private final IndexFig indexFig;
    private final Timer timer;


    @Inject
    public IndexRefreshCommandImpl(
                                    final EsProvider esProvider,
                                    final IndexProducer producer,
                                    final IndexFig indexFig,
                                    final MetricsFactory metricsFactory,
                                    final IndexCache indexCache ) {


        this.timer = metricsFactory.getTimer( IndexRefreshCommandImpl.class, "index.refresh" );

        this.esProvider = esProvider;
        this.producer = producer;
        this.indexFig = indexFig;
        this.indexCache = indexCache;
    }


    @Override
    public Observable<IndexRefreshCommandInfo> execute(IndexAlias alias, String[] indexes ) {

        final long start = System.currentTimeMillis();


        //id to hunt for
        final UUID uuid = UUIDUtils.newTimeUUID();
        final Entity entity = new Entity( new SimpleId( uuid, "ug_refresh_index_type" ) );
        EntityUtils.setVersion( entity, UUIDGenerator.newTimeUUID() );
        final Id appId = new SimpleId( "ug_refresh_index" );
        final ApplicationScope appScope = new ApplicationScopeImpl( appId );
        final IndexEdge edge = new IndexEdgeImpl( appId, "refresh", SearchEdge.NodeType.SOURCE, uuid.timestamp() );
        final String docId = IndexingUtils.createIndexDocId( appScope, entity, edge );
        final Map<String, Object> entityData = EntityToMapConverter.convert( appScope, edge, entity );
        final String entityId = entityData.get( IndexingUtils.ENTITY_ID_FIELDNAME ).toString();
        //add a tracer record
        IndexOperation indexRequest = new IndexOperation( alias.getWriteAlias(), docId, entityData );
        //save the item
        final IndexOperationMessage message = new IndexOperationMessage();
        message.addIndexRequest( indexRequest );

        //add the record to the index
        final Observable<IndexOperationMessage> addRecord = producer.put( message );

        //refresh the index
        //        final Observable<Boolean> refresh = refresh( indexes );

        /**
         * We have to search.  Get by ID returns immediately, even if search isn't ready, therefore we have to search
         */
        //set our filter for entityId fieldname


        /**
         * We want to search once we've added our record, then refreshed
         */
        final Observable<IndexRefreshCommandInfo> searchObservable =
            Observable.create(sub -> {
                try {
                    IndexRefreshCommandInfo info = null;
                    for(int i = 0; i<indexFig.maxRefreshSearches();i++) {
                        final SearchRequestBuilder builder = esProvider.getClient().prepareSearch(alias.getReadAlias())
                            .setTypes(IndexingUtils.ES_ENTITY_TYPE)
                            .setPostFilter(FilterBuilders
                                .termFilter(IndexingUtils.ENTITY_ID_FIELDNAME,
                                    entityId));

                        info = new IndexRefreshCommandInfo(builder.execute().get().getHits().totalHits() > 0,
                            System.currentTimeMillis() - start);
                        if(info.hasFinished()){
                            break;
                        }else {
                            Thread.sleep(50);
                        }
                    }
                    sub.onNext(info);
                    sub.onCompleted();
                } catch (Exception ee) {
                    logger.error("Failed during refresh search for " + uuid, ee);
                    throw new RuntimeException("Failed during refresh search for " + uuid, ee);
                }
            });


        //chain it all together

        //add the record, take it's last result.  On the last add, we then execute the refresh command

        final Observable<IndexRefreshCommandInfo> refreshResults = addRecord

            //after our add, run a refresh
            .doOnNext( addResult -> {


                if ( indexes.length == 0 ) {
                    logger.debug( "Not refreshing indexes. none found" );
                }
                //Added For Graphite Metrics
                RefreshResponse response =
                    esProvider.getClient().admin().indices().prepareRefresh( indexes ).execute().actionGet();
                int failedShards = response.getFailedShards();
                int successfulShards = response.getSuccessfulShards();
                ShardOperationFailedException[] sfes = response.getShardFailures();
                if ( sfes != null ) {
                    for ( ShardOperationFailedException sfe : sfes ) {
                        logger.error( "Failed to refresh index:{} reason:{}", sfe.index(), sfe.reason() );
                    }
                }
                logger.debug( "Refreshed indexes: {},success:{} failed:{} ", StringUtils.join( indexes, ", " ),
                    successfulShards, failedShards);
            })

                //once the refresh is done execute the search
            .flatMap(refreshCommandResult -> searchObservable)

                //check when found
            .doOnNext(found -> {
                if (!found.hasFinished()) {
                    logger.error("Couldn't find record during refresh uuid: {} took ms:{} ", uuid,
                        found.getExecutionTime());
                } else {
                    logger.info("found record during refresh uuid: {} took ms:{} ", uuid, found.getExecutionTime());
                }
            }).doOnCompleted(() -> {
                //clean up our data
                String[] aliases = indexCache.getIndexes(alias, EntityIndex.AliasType.Read);
                DeIndexOperation deIndexRequest =
                    new DeIndexOperation(aliases, appScope, edge, entity.getId(), entity.getVersion());

                //delete the item
                IndexOperationMessage indexOperationMessage = new IndexOperationMessage();
                indexOperationMessage.addDeIndexRequest( deIndexRequest );
                producer.put( indexOperationMessage );
            } );


        return ObservableTimer.time( refreshResults, timer ) ;
    }
}
