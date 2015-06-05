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


import java.util.concurrent.ExecutionException;

import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
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
    private final EsProvider provider;
    private final IndexBufferConsumer indexBatchBufferProducer;
    private final MetricsFactory metricsFactory;
    private final AliasedEntityIndex entityIndex;

    private LoadingCache<IndexLocationStrategy, ApplicationEntityIndex> eiCache =
        CacheBuilder.newBuilder().maximumSize( 1000 ).build( new CacheLoader<IndexLocationStrategy, ApplicationEntityIndex>() {
            public ApplicationEntityIndex load( IndexLocationStrategy locationStrategy ) {
                return new EsApplicationEntityIndexImpl(
                    entityIndex,config, indexBatchBufferProducer, provider, metricsFactory, locationStrategy
                );
            }
        } );

    @Inject
    public EsEntityIndexFactoryImpl( final IndexFig indexFig, final EsProvider provider,
                                     final IndexBufferConsumer indexBatchBufferProducer,
                                     final MetricsFactory metricsFactory,
                                     final AliasedEntityIndex entityIndex

    ){
        this.config = indexFig;
        this.provider = provider;
        this.indexBatchBufferProducer = indexBatchBufferProducer;
        this.metricsFactory = metricsFactory;
        this.entityIndex = entityIndex;
    }



    @Override
    public ApplicationEntityIndex createApplicationEntityIndex(final IndexLocationStrategy indexLocationStrategy) {
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
