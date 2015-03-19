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
package org.apache.usergrid.persistence.graph.serialization.impl;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.Assisted;

import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.impl.GraphManagerImpl;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeDeleteListener;
import org.apache.usergrid.persistence.graph.impl.stage.NodeDeleteListener;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.NodeSerialization;

import java.util.concurrent.ExecutionException;

/**
 * Returns Graph Managers, built to manage the caching
 */
@Singleton
public class GraphManagerFactoryImpl implements GraphManagerFactory {

    private final EdgeMetadataSerialization edgeMetadataSerialization;
    private final EdgeSerialization edgeSerialization;
    private final NodeSerialization nodeSerialization;
    private final GraphFig graphFig;
    private final EdgeDeleteListener edgeDeleteListener;
    private final NodeDeleteListener nodeDeleteListener;
    private final MetricsFactory metricsFactory;

    private LoadingCache<ApplicationScope, GraphManager> gmCache =
        CacheBuilder.newBuilder().maximumSize( 1000 ).build( new CacheLoader<ApplicationScope, GraphManager>() {
            public GraphManager load(
                ApplicationScope scope ) {
                return new GraphManagerImpl(edgeMetadataSerialization,edgeSerialization,nodeSerialization,graphFig,edgeDeleteListener,nodeDeleteListener,scope, metricsFactory);
            }
        } );

    @Inject
    public GraphManagerFactoryImpl( final EdgeMetadataSerialization edgeMetadataSerialization, final
    EdgeSerialization edgeSerialization,
                                    final NodeSerialization nodeSerialization, final GraphFig graphFig, final EdgeDeleteListener edgeDeleteListener,
                                    final NodeDeleteListener nodeDeleteListener, final MetricsFactory metricsFactory ){
        this.edgeMetadataSerialization = edgeMetadataSerialization;
        this.edgeSerialization = edgeSerialization;
        this.nodeSerialization = nodeSerialization;
        this.graphFig = graphFig;
        this.edgeDeleteListener = edgeDeleteListener;
        this.nodeDeleteListener = nodeDeleteListener;
        this.metricsFactory = metricsFactory;
    }

    @Override
    public GraphManager createEdgeManager(ApplicationScope collectionScope) {
        Preconditions.checkNotNull(collectionScope);

        try {
            return gmCache.get(collectionScope);
        }catch (ExecutionException ee){
            throw new RuntimeException(ee);
        }
    }

    @Override
    public void invalidate() {
        gmCache.invalidateAll();
    }
}
