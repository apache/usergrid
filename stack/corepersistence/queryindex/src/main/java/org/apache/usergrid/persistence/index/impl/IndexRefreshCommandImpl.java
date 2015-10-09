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

        return ObservableTimer.time(Observable.just(refreshResults), timer);
    }
}
