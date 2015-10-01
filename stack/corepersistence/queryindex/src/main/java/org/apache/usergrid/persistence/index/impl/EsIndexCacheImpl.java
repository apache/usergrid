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


import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.usergrid.persistence.index.*;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * Cache for Es index operations
 */
@Singleton
public class EsIndexCacheImpl implements IndexCache {

    private static final Logger logger = LoggerFactory.getLogger( EsEntityIndexImpl.class );
    private final ListeningScheduledExecutorService refreshExecutors;

    private LoadingCache<String, String[]> aliasIndexCache;
    private EsProvider provider;


    @Inject
    public EsIndexCacheImpl( final EsProvider provider, final IndexFig indexFig ) {

        this.refreshExecutors =
            MoreExecutors.listeningDecorator( Executors.newScheduledThreadPool( indexFig.getIndexCacheMaxWorkers() ) );

        this.provider = provider;

        aliasIndexCache = CacheBuilder.newBuilder().maximumSize( 1000 ).refreshAfterWrite( 5, TimeUnit.MINUTES )
                                      .build( new CacheLoader<String, String[]>() {
                                          @Override
                                          public ListenableFuture<String[]> reload( final String key,
                                                                                    String[] oldValue )
                                              throws Exception {
                                              ListenableFutureTask<String[]> task =
                                                  ListenableFutureTask.create( new Callable<String[]>() {
                                                      public String[] call() {
                                                          return load( key );
                                                      }
                                                  } );
                                              refreshExecutors.execute( task );
                                              return task;
                                          }


                                          @Override
                                          public String[] load( final String aliasName ) {
                                             return getIndexesFromEs(aliasName);
                                          }
                                      } );
    }


    /**
     * Get indexes for an alias
     */
    @Override
    public String[] getIndexes(IndexAlias alias, EntityIndex.AliasType aliasType) {
        String[] indexes;
        try {
            indexes = aliasIndexCache.get( getAliasName( alias, aliasType ) );
        }
        catch ( ExecutionException ee ) {
            logger.error( "Failed to retreive indexes", ee );
            throw new RuntimeException( ee );
        }
        return indexes;
    }



    private String[] getIndexesFromEs(final String aliasName){
        final AdminClient adminClient = this.provider.getClient().admin();
             //remove write alias, can only have one
        ImmutableOpenMap<String, List<AliasMetaData>> aliasMap =
            adminClient.indices().getAliases( new GetAliasesRequest( aliasName ) ).actionGet().getAliases();
        return aliasMap.keys().toArray( String.class );
    }


    /**
     * Get the name of the alias to use
     * @param alias
     * @param aliasType
     * @return
     */
    private String getAliasName( IndexAlias alias, EntityIndex.AliasType aliasType ) {
        return aliasType == EntityIndex.AliasType.Read ? alias.getReadAlias() : alias.getWriteAlias();
    }


    /**
     * clean up cache
     */
    @Override
    public void invalidate(IndexAlias alias) {
        aliasIndexCache.invalidate( alias.getWriteAlias() );
        aliasIndexCache.invalidate( alias.getReadAlias() );
    }
}
