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

import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.index.*;
import org.apache.usergrid.persistence.index.query.ParsedQuery;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.EntityUtils;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;

import java.util.UUID;

/**
 * Classy class class.
 */
public  class IndexRefreshCommandImpl implements IndexRefreshCommand {
    private static final Logger logger = LoggerFactory.getLogger(IndexRefreshCommandImpl.class);

    private final IndexAlias alias;
    private final EsProvider esProvider;
    private final IndexBufferProducer producer;
    private final IndexFig indexFig;
    private final Timer timer;

    @Inject
    public IndexRefreshCommandImpl(FailureMonitorImpl.IndexIdentifier indexIdentifier, EsProvider esProvider,IndexBufferProducer producer,IndexFig indexFig, MetricsFactory metricsFactory){

        this.timer = metricsFactory.getTimer(IndexRefreshCommandImpl.class,"index.refresh.timer");
        this.alias = indexIdentifier.getAlias();
        this.esProvider = esProvider;
        this.producer = producer;
        this.indexFig = indexFig;
    }
    @Override
    public Observable<Boolean> execute() {

        Timer.Context refreshTimer = timer.time();
        //id to hunt for
        final UUID uuid = UUIDUtils.newTimeUUID();

        final Entity entity = new Entity(new SimpleId(uuid,"ug_refresh_index_type"));
        EntityUtils.setVersion(entity, UUIDGenerator.newTimeUUID());
        final Id appId = new SimpleId("ug_refresh_index");
        final ApplicationScope appScope = new ApplicationScopeImpl(appId);
        final IndexEdge edge = new IndexEdgeImpl(appId,"refresh", SearchEdge.NodeType.SOURCE,uuid.timestamp());

        //add a tracer record
        org.apache.usergrid.persistence.index.impl.IndexRequest indexRequest =
            new org.apache.usergrid.persistence.index.impl.IndexRequest(
                alias.getWriteAlias(),appScope,edge,entity
            );

        //save the item
        IndexIdentifierImpl.IndexOperationMessage message = new IndexIdentifierImpl.IndexOperationMessage();
        message.addIndexRequest(indexRequest);
        producer.put(message);

        //initialize search for item
        SearchRequestBuilderStrategy srb = new SearchRequestBuilderStrategy(esProvider,appScope,alias,0);
        ParsedQuery query = new ParsedQuery();
        query.addSelect(String.format("select * where uuid='{}'", uuid.toString()));
        SearchRequestBuilder builder = srb.getBuilder(edge, SearchTypes.fromTypes(entity.getId().getType()), query, 1);

        Observable<Boolean> observable = Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                try {
                    boolean found = false;
                    for (int i = 0; i < indexFig.maxRefreshSearches() && !found; i++) {
                        SearchResponse response = builder.execute().get();
                        if (response.getHits().hits().length > 0) {
                            found = true;
                            break;
                        } else {
                            Thread.sleep(indexFig.refreshSleep());
                        }
                    }
                    subscriber.onNext(found);
                } catch (Exception ee) {
                    logger.error("Failed during refresh search for " + uuid.toString(), ee);
                    subscriber.onError(ee);
                } finally {
                    subscriber.onCompleted();
                }
            }
        }).doOnNext(found -> {
            if (!found) {
                logger.error("Couldn't find record during refresh uuid" + uuid);
            }
        })
            .doOnCompleted(()->refreshTimer.stop());
        return observable;
    }
}
