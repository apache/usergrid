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
package org.apache.usergrid.corepersistence.service;

import com.codahale.metrics.Timer;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.apache.usergrid.corepersistence.index.IndexLocationStrategyFactory;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.metrics.ObservableTimer;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.IndexLocationStrategy;
import org.apache.usergrid.persistence.index.SearchEdge;
import rx.observables.MathObservable;

import java.util.*;

/**
 * Aggregation Service get counts for an application
 */
public class AggregationServiceImpl implements AggregationService {

    private final EntityIndexFactory entityIndexFactory;
    private final IndexLocationStrategyFactory indexLocationStrategyFactory;
    private final GraphManagerFactory graphManagerFactory;
    private final MetricsFactory metricsFactory;
    private final Timer sumTimer;

    @Inject
    public AggregationServiceImpl(
        final EntityIndexFactory entityIndexFactory,
        final IndexLocationStrategyFactory indexLocationStrategyFactory,
        final GraphManagerFactory graphManagerFactory,
        final MetricsFactory metricsFactory){

        this.entityIndexFactory = entityIndexFactory;
        this.indexLocationStrategyFactory = indexLocationStrategyFactory;
        this.graphManagerFactory = graphManagerFactory;
        this.metricsFactory = metricsFactory;
        this.sumTimer = metricsFactory.getTimer(AggregationServiceImpl.class,"sum");
    }


    @Override
    public long getApplicationSize(ApplicationScope applicationScope) {
        final IndexLocationStrategy indexLocationStrategy = indexLocationStrategyFactory.getIndexLocationStrategy(applicationScope);
        EntityIndex entityIndex = entityIndexFactory.createEntityIndex(indexLocationStrategy);
        GraphManager graphManager = graphManagerFactory.createEdgeManager(applicationScope);
        Long sum = ObservableTimer.time(
            MathObservable.sumLong(
                graphManager.getEdgeTypesFromSource(new SimpleSearchEdgeType(applicationScope.getApplication(), CpNamingUtils.EDGE_COLL_PREFIX, Optional.<String>absent()))
                    .map(type -> CpNamingUtils.createCollectionSearchEdge(applicationScope.getApplication(), type))
                    .map(edge -> entityIndex.getEntitySize(edge))
            ), sumTimer).toBlocking().last();

        return sum.longValue();
    }

    @Override
    public Map<String, Long> getEachCollectionSize(ApplicationScope applicationScope) {
        final IndexLocationStrategy indexLocationStrategy = indexLocationStrategyFactory.getIndexLocationStrategy(applicationScope);
        EntityIndex entityIndex = entityIndexFactory.createEntityIndex(indexLocationStrategy);
        GraphManager graphManager = graphManagerFactory.createEdgeManager(applicationScope);
        Map<String,Long> sumMap = ObservableTimer.time(
                graphManager.getEdgeTypesFromSource(new SimpleSearchEdgeType(applicationScope.getApplication(), CpNamingUtils.EDGE_COLL_PREFIX, Optional.<String>absent()))
                    .collect(() -> new HashMap<String,Long>(), ((map, type) ->
                        {
                            SearchEdge edge = CpNamingUtils.createCollectionSearchEdge(applicationScope.getApplication(), type);
                            final String collectionName = CpNamingUtils.getCollectionNameFromEdgeName(type);
                            long sumType =  entityIndex.getEntitySize(edge);
                            map.put(collectionName,sumType);
                        })
                    )
            , sumTimer).toBlocking().last();
        return sumMap;

    }

    @Override
    public long getSize(ApplicationScope applicationScope, SearchEdge edge) {
        final IndexLocationStrategy indexLocationStrategy = indexLocationStrategyFactory.getIndexLocationStrategy(applicationScope);
        EntityIndex entityIndex = entityIndexFactory.createEntityIndex(indexLocationStrategy);
        return entityIndex.getEntitySize(edge);
    }

    @Override
    public long getCollectionSize(final ApplicationScope applicationScope, final String collectionName) {
        return getSize(applicationScope, CpNamingUtils.createCollectionSearchEdge(applicationScope.getApplication(), collectionName));
    }

}
