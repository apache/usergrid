/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.persistence.cache.impl;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.persistence.cache.CacheFactory;
import org.apache.usergrid.persistence.cache.CacheScope;
import org.apache.usergrid.persistence.cache.ScopedCache;

import java.util.concurrent.ExecutionException;

/**
 * Access to caches.
 */
@Singleton
public class CacheFactoryImpl<K, V> implements CacheFactory<K, V> {

    private LoadingCache<CacheScope, ScopedCache> cacheCache;

    @Inject
    public CacheFactoryImpl( final ScopedCacheSerialization serializer ) {

        cacheCache = CacheBuilder.newBuilder().maximumSize(1000).build(
            new CacheLoader<CacheScope, ScopedCache>() {
                public ScopedCache load(CacheScope scope) {
                    return new ScopedCacheImpl(scope, serializer);
                }
            });
    }


    @Override
    public ScopedCache<K, V> getScopedCache(CacheScope scope) {
        Preconditions.checkNotNull(scope);
        try{
            return cacheCache.get(scope);
        } catch (ExecutionException ee) {
            throw new RuntimeException(ee);
        }
    }
}
