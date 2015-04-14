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
import org.apache.usergrid.persistence.index.AliasedEntityIndex;
import org.apache.usergrid.persistence.index.ApplicationEntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.map.MapManagerFactory;

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
    private final IndexBufferProducer indexBatchBufferProducer;
    private final MetricsFactory metricsFactory;
    private final MapManagerFactory mapManagerFactory;
    private final IndexFig indexFig;
    private final AliasedEntityIndex entityIndex;
    private final FailureMonitorImpl.IndexIdentifier indexIdentifier;

    private LoadingCache<ApplicationScope, ApplicationEntityIndex> eiCache =
        CacheBuilder.newBuilder().maximumSize( 1000 ).build( new CacheLoader<ApplicationScope, ApplicationEntityIndex>() {
            public ApplicationEntityIndex load( ApplicationScope scope ) {
                return new EsApplicationEntityIndexImpl(
                    scope,entityIndex,config, indexBatchBufferProducer, provider, metricsFactory, mapManagerFactory, indexFig, indexIdentifier
                );
            }
        } );

    @Inject
    public EsEntityIndexFactoryImpl( final IndexFig config, final EsProvider provider,
                                     final IndexBufferProducer indexBatchBufferProducer,
                                     final MetricsFactory metricsFactory, final MapManagerFactory mapManagerFactory,
                                     final IndexFig indexFig, final AliasedEntityIndex entityIndex, final FailureMonitorImpl.IndexIdentifier indexIdentifier ){
        this.config = config;
        this.provider = provider;
        this.indexBatchBufferProducer = indexBatchBufferProducer;
        this.metricsFactory = metricsFactory;
        this.mapManagerFactory = mapManagerFactory;
        this.indexFig = indexFig;
        this.entityIndex = entityIndex;
        this.indexIdentifier = indexIdentifier;
    }



    @Override
    public ApplicationEntityIndex createApplicationEntityIndex(final ApplicationScope appScope) {
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
