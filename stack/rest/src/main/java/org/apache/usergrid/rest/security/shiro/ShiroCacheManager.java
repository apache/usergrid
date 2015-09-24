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
package org.apache.usergrid.rest.security.shiro;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.cache.CacheManager;
import org.apache.usergrid.persistence.cache.CacheFactory;
import org.apache.usergrid.security.shiro.UsergridAuthenticationInfo;
import org.apache.usergrid.security.shiro.UsergridAuthorizationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * Plugin Usergrid cache for Shiro.
 */
public class ShiroCacheManager implements CacheManager {

    private static final Logger logger = LoggerFactory.getLogger( ShiroCacheManager.class );

    @Autowired
    protected Injector injector;

    Map<String, ShiroCache> caches = new HashMap<>();

    public ShiroCacheManager() {
    }

    @Override
    public <K, V> Cache<K, V> getCache(String name) throws CacheException {
        ShiroCache shiroCache = caches.get(name);

        if ( shiroCache == null ) {

            if ( "realm.authorizationCache".equals(name) ) {

                shiroCache = new ShiroCache(
                    new TypeReference<UsergridAuthorizationInfo>(){},
                    injector.getInstance(
                        Key.get(new TypeLiteral<CacheFactory<String, UsergridAuthorizationInfo>>() { })));

            } else if ( "realm.authenticationCache".equals(name)) {

                shiroCache = new ShiroCache(
                    new TypeReference<UsergridAuthenticationInfo>(){},
                    injector.getInstance(
                        Key.get(new TypeLiteral<CacheFactory<String, UsergridAuthenticationInfo>>() { })));

            } else {
                throw new RuntimeException("Unknown Shiro cache name");
            }

            caches.put( name, shiroCache );
        }
        return shiroCache;
    }

    public void invalidateApplicationCaches( UUID applicationId ) {
        for ( String key : caches.keySet() ) {
            ShiroCache shiroCache = caches.get( key );
            shiroCache.invalidate(applicationId);
        }
    }
}
