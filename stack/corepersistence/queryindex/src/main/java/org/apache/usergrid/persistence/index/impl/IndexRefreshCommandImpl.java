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


import java.util.Map;
import java.util.UUID;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.rx.RxTaskScheduler;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.index.AliasedEntityIndex;
import org.apache.usergrid.persistence.index.IndexEdge;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.IndexRefreshCommand;
import org.apache.usergrid.persistence.index.SearchEdge;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.EntityUtils;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.codahale.metrics.Timer;
import com.google.inject.Inject;

import rx.Observable;
import rx.util.async.Async;


/**
 * Classy class class.
 */
public class IndexRefreshCommandImpl implements IndexRefreshCommand {
    private static final Logger logger = LoggerFactory.getLogger( IndexRefreshCommandImpl.class );

    private final IndexAlias alias;
    private final IndexCache indexCache;
    private final EsProvider esProvider;
    private final IndexBufferConsumer producer;
    private final IndexFig indexFig;
    private final Timer timer;
    private final RxTaskScheduler rxTaskScheduler;

    @Inject
    public IndexRefreshCommandImpl( IndexIdentifier indexIdentifier, EsProvider esProvider,
                                    IndexBufferConsumer producer, IndexFig indexFig, MetricsFactory metricsFactory,
                                    final IndexCache indexCache, final RxTaskScheduler rxTaskScheduler ) {



        this.timer = metricsFactory.getTimer( IndexRefreshCommandImpl.class, "index.refresh.timer" );
        this.alias = indexIdentifier.getAlias();
        this.esProvider = esProvider;
        this.producer = producer;
        this.indexFig = indexFig;
        this.indexCache = indexCache;
        this.rxTaskScheduler = rxTaskScheduler;
    }


    @Override
    public Observable<IndexRefreshCommandInfo> execute() {
        final long start = System.currentTimeMillis();

        Timer.Context refreshTimer = timer.time();
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
        IndexOperationMessage message = new IndexOperationMessage();
        message.addIndexRequest( indexRequest );
        producer.put( message );

        /**
         * We have to search.  Get by ID returns immediately, even if search isn't ready, therefore we have to search
         */

        final SearchRequestBuilder builder =
            esProvider.getClient().prepareSearch(alias.getReadAlias()).setTypes(IndexingUtils.ES_ENTITY_TYPE)

                //set our filter for entityId fieldname
        .setPostFilter(FilterBuilders.termFilter(IndexingUtils.ENTITY_ID_FIELDNAME, entityId));


        //start our processing immediately
        final Observable<IndexRefreshCommandInfo> future = Async.toAsync( () -> {
            try {
                boolean found = false;
                for ( int i = 0; i < indexFig.maxRefreshSearches(); i++ ) {
                    Thread.sleep(indexFig.refreshSleep());

                    final SearchResponse response = builder.execute().get();

                    if (response.getHits().totalHits() > 0) {
                        found = true;
                        break;
                    }

                }

                return new IndexRefreshCommandInfo(found,System.currentTimeMillis() - start);
            }
            catch ( Exception ee ) {
                logger.error( "Failed during refresh search for " + uuid, ee );
                throw new RuntimeException( "Failed during refresh search for " + uuid, ee );
            }
        }, rxTaskScheduler.getAsyncIOScheduler() ).call();


        return future.doOnNext( found -> {
            if ( !found.hasFinished() ) {
                logger.error("Couldn't find record during refresh uuid: {} took ms:{} ", uuid, found.getExecutionTime());
            }else{
                logger.info("found record during refresh uuid: {} took ms:{} ", uuid, found.getExecutionTime());
            }
        } ).doOnCompleted(() -> {
            //clean up our data
            String[] aliases = indexCache.getIndexes(alias, AliasedEntityIndex.AliasType.Read);
            DeIndexOperation deIndexRequest =
                new DeIndexOperation(aliases, appScope, edge, entity.getId(), entity.getVersion());

            //delete the item
            IndexOperationMessage indexOperationMessage =
                new IndexOperationMessage();
            indexOperationMessage.addDeIndexRequest( deIndexRequest );
            producer.put( indexOperationMessage );

            refreshTimer.stop();
        });
    }
}
