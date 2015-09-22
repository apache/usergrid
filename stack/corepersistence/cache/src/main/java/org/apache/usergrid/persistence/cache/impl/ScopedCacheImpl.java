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

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.Inject;
import org.apache.usergrid.persistence.cache.CacheScope;
import org.apache.usergrid.persistence.cache.ScopedCache;


/**
 * Cache divided into scopes which can be individually invalidated.
 */
public class ScopedCacheImpl<K,V> implements ScopedCache<K,V> {

    CacheScope scope;

    ScopedCacheSerialization<K,V> serializer;

    public ScopedCacheImpl( CacheScope scope, ScopedCacheSerialization<K,V> serializer ) {
        this.scope = scope;
        this.serializer = serializer;
    }

    @Override
    public V put(K key, V value, Integer ttl) {
        return serializer.writeValue( scope, key, value, ttl );
    }

    @Override
    public V get(K key, TypeReference typeRef ) {
        return serializer.readValue( scope, key, typeRef );
    }

    public void remove( K key ) {
        serializer.removeValue( scope, key );
    }

    @Override
    public void invalidate() {
        serializer.invalidate(scope);
    }
}
