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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;

import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.IndexBufferProducer;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.map.MapManagerFactory;

import java.util.concurrent.ExecutionException;

/**
 * Get index from factory, adds caching
 */
public class EsEntityIndexFactoryImpl implements EntityIndexFactory{

    private final IndexFig config;
    private final EsProvider provider;
    private final EsIndexCache indexCache;
    private final IndexBufferProducer indexBatchBufferProducer;
    private final MetricsFactory metricsFactory;
    private final MapManagerFactory mapManagerFactory;
    private final IndexFig indexFig;

    private LoadingCache<ApplicationScope, EntityIndex> eiCache =
        CacheBuilder.newBuilder().maximumSize( 1000 ).build( new CacheLoader<ApplicationScope, EntityIndex>() {
            public EntityIndex load( ApplicationScope scope ) {
                return new EsEntityIndexImpl(scope,config, indexBatchBufferProducer, provider,indexCache, metricsFactory,
                    mapManagerFactory, indexFig );
            }
        } );

    @Inject
    public EsEntityIndexFactoryImpl( final IndexFig config, final EsProvider provider, final EsIndexCache indexCache,
                                     final IndexBufferProducer indexBatchBufferProducer,
                                     final MetricsFactory metricsFactory, final MapManagerFactory mapManagerFactory,
                                     final IndexFig indexFig ){
        this.config = config;
        this.provider = provider;
        this.indexCache = indexCache;
        this.indexBatchBufferProducer = indexBatchBufferProducer;
        this.metricsFactory = metricsFactory;
        this.mapManagerFactory = mapManagerFactory;
        this.indexFig = indexFig;
    }

    @Override
    public EntityIndex createEntityIndex(final ApplicationScope appScope) {
        try{
            return eiCache.get(appScope);
        }catch (ExecutionException ee){
            throw new RuntimeException(ee);
        }
    }

    @Override
    public void invalidate() {
        eiCache.invalidateAll();
    }
}
