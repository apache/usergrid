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
package org.apache.usergrid.security.shiro.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Singleton;
import org.apache.usergrid.security.shiro.ShiroCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;


@Singleton
public class LocalShiroCache<K, V> {

    private final Cache<K,V> cache;

    private static final String CACHE_TTL_PROP = "usergrid.auth.cache.inmemory.time-to-live";
    private static final String CACHE_MAX_SIZE_PROP = "usergrid.auth.cache.inmemory.size";


    public LocalShiroCache(){

        // set a smaller ttl
        long ttl = 1;
        int configuredMaxSize;

        try{
            ttl = Integer.parseInt(System.getProperty(CACHE_TTL_PROP));
        } catch (NumberFormatException e){
           // already defaulted to 1 above
        }

        try{
            configuredMaxSize = Integer.parseInt(System.getProperty(CACHE_MAX_SIZE_PROP));
        } catch (NumberFormatException e){
            configuredMaxSize = 1000;
        }

        this.cache = CacheBuilder.newBuilder()
            .maximumSize(Math.max(1000,configuredMaxSize))
            .expireAfterWrite(ttl, TimeUnit.SECONDS).build();

    }

    public void put(K key, V value){

        cache.put(key, value);
    }


    public V get(K key){
        return cache.getIfPresent(key);
    }

    public void invalidate(K key){
        cache.invalidate(key);
    }

    public void invalidateAll(){
        cache.invalidateAll();
    }

}
