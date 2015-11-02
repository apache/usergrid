/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.index.impl;


import java.util.concurrent.ExecutionException;

import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.index.*;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;

/**
 * Get index from factory, adds caching
 */
public class EsEntityIndexFactoryImpl implements EntityIndexFactory{

    private final IndexFig config;
    private final IndexCache indexCache;
    private final EsProvider provider;
    private final MetricsFactory metricsFactory;

    private LoadingCache<IndexLocationStrategy, EntityIndex> eiCache =
        CacheBuilder.newBuilder().maximumSize( 1000 ).build( new CacheLoader<IndexLocationStrategy, EntityIndex>() {
            public EntityIndex load( IndexLocationStrategy locationStrategy ) {
                EntityIndex index =  new EsEntityIndexImpl(
                    provider,
                    indexCache,
                    config,
                    metricsFactory,
                    locationStrategy
                );
                index.initialize();
                return index;
            }
        } );

    @Inject
    public EsEntityIndexFactoryImpl( final IndexFig indexFig,
                                     final IndexCache indexCache,
                                     final EsProvider provider,
                                     final MetricsFactory metricsFactory

    ){
        this.config = indexFig;
        this.indexCache = indexCache;
        this.provider = provider;
        this.metricsFactory = metricsFactory;
    }



    @Override
    public EntityIndex createEntityIndex(final IndexLocationStrategy indexLocationStrategy) {
        try{
            return eiCache.get(indexLocationStrategy);
        }catch (ExecutionException ee){
            throw new RuntimeException(ee);
        }
    }

    @Override
    public void invalidate() {
        eiCache.invalidateAll();
    }
}
