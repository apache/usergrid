package org.apache.usergrid.corepersistence.index;

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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.concurrent.TimeUnit;

@Singleton
public class CollectionSettingsCache {

    private final Cache<CollectionSettingsScope,String> cache;


    @Inject
    public CollectionSettingsCache( CollectionSettingsCacheFig fig ) {
        this.cache = CacheBuilder.newBuilder()
            .maximumSize(fig.getCacheSize())
            .expireAfterWrite(fig.getCacheTimeout(), TimeUnit.SECONDS).build();
    }


    public void put(CollectionSettingsScope key, String value){

        cache.put(key, value);
    }


    public String get(CollectionSettingsScope key){
        return cache.getIfPresent(key);
    }

    public void invalidate(CollectionSettingsScope key){
        cache.invalidate(key);
    }

    public void invalidateAll(){
        cache.invalidateAll();
    }

}
