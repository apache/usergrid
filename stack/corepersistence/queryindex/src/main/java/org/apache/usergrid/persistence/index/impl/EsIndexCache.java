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
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.persistence.index.AliasedEntityIndex;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.IndexIdentifier;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Cache for Es index operations
 */
@Singleton
public class EsIndexCache {

    private static final Logger logger = LoggerFactory.getLogger(EsEntityIndexImpl.class);
    private final ListeningScheduledExecutorService refreshExecutors;

    private LoadingCache<String, String[]> aliasIndexCache;

    @Inject
    public EsIndexCache(final EsProvider provider, final IndexFig indexFig) {
        this.refreshExecutors = MoreExecutors
                .listeningDecorator(Executors.newScheduledThreadPool(indexFig.getIndexCacheMaxWorkers()));
        aliasIndexCache = CacheBuilder.newBuilder().maximumSize(1000)
                .refreshAfterWrite(5,TimeUnit.MINUTES)
                .build(new CacheLoader<String, String[]>() {
                    @Override
                    public ListenableFuture<String[]> reload(final String key, String[] oldValue) throws Exception {
                        ListenableFutureTask<String[]> task = ListenableFutureTask.create( new Callable<String[]>() {
                            public String[] call() {
                                return load( key );
                            }
                        } );
                        refreshExecutors.execute(task);
                        return task;
                    }

                    @Override
                    public String[] load(final String aliasName) {
                        final AdminClient adminClient = provider.getClient().admin();
                        //remove write alias, can only have one
                        ImmutableOpenMap<String, List<AliasMetaData>> aliasMap = adminClient.indices().getAliases(new GetAliasesRequest(aliasName)).actionGet().getAliases();
                        return aliasMap.keys().toArray(String.class);
                    }
                }) ;
    }

    /**
     * Get indexes for an alias
     * @param alias
     * @param aliasType
     * @return
     */
    public String[] getIndexes(IndexIdentifier.IndexAlias alias, AliasedEntityIndex.AliasType aliasType) {
        String[] indexes;
        try {
            indexes = aliasIndexCache.get(aliasType == AliasedEntityIndex.AliasType.Read ? alias.getReadAlias() : alias.getWriteAlias());
        } catch (ExecutionException ee) {
            logger.error("Failed to retreive indexes", ee);
            throw new RuntimeException(ee);
        }
        return indexes;
    }

    /**
     * clean up cache
     * @param alias
     */
    public void invalidate(IndexIdentifier.IndexAlias alias){
        aliasIndexCache.invalidate(alias.getWriteAlias());
        aliasIndexCache.invalidate(alias.getReadAlias());

    }

}
