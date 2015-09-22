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
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.usergrid.persistence.cache.CacheFactory;
import org.apache.usergrid.persistence.cache.CacheScope;
import org.apache.usergrid.persistence.cache.ScopedCache;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.security.shiro.principals.AdminUserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;


/**
 * Plugin Usergrid cache for Shiro.
 */
public class ShiroCache<K, V> implements Cache<K,V> {

    private static final Logger logger = LoggerFactory.getLogger( ShiroCacheManager.class );

    CacheFactory<String, V> cacheFactory;

    TypeReference typeRef = new TypeReference<SimpleAuthorizationInfo>() {};


    public ShiroCache( CacheFactory<String, V> cacheFactory ) {
        this.cacheFactory = cacheFactory;
    }

    @Override
    public V get(K key) throws CacheException {
        ScopedCache<String, V> scopedCache = getCacheScope(key);
        if ( scopedCache != null ) {
            return scopedCache.get( getKeyString(key), typeRef);
        }
        return null;
    }

    @Override
    public V put(K key, V value) throws CacheException {
        ScopedCache<String, V> scopedCache = getCacheScope(key);
        if ( scopedCache != null ) {
            return scopedCache.put( getKeyString(key) , value, 5000);
        }
        return null;
    }

    @Override
    public V remove(K key) throws CacheException {
        ScopedCache<String, V> scopedCache = getCacheScope(key);
        if ( scopedCache != null ) {
            scopedCache.remove( getKeyString(key) );
        }
        return null;
    }

    @Override
    public void clear() throws CacheException {
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Set<K> keys() {
        return Collections.EMPTY_SET;
    }

    @Override
    public Collection<V> values() {
        return Collections.EMPTY_LIST;
    }


    /** get cache for application scope */
    private ScopedCache<String, V> getCacheScope( K key ) {

        if ( key instanceof SimplePrincipalCollection) {
            SimplePrincipalCollection spc = (SimplePrincipalCollection)key;

            if ( spc.getPrimaryPrincipal() instanceof AdminUserPrincipal ) {
                AdminUserPrincipal p = (AdminUserPrincipal) spc.getPrimaryPrincipal();
                CacheScope scope = new CacheScope(new SimpleId(p.getApplicationId(), "application"));
                ScopedCache<String, V> scopedCache = cacheFactory.getScopedCache(scope);
                return scopedCache;
            }
        }
        return null;
    }


    /** key is the application UUID in string form */
    private String getKeyString( K key ) {

        if ( key instanceof SimplePrincipalCollection) {
            SimplePrincipalCollection spc = (SimplePrincipalCollection)key;

            if ( spc.getPrimaryPrincipal() instanceof AdminUserPrincipal ) {
                AdminUserPrincipal p = (AdminUserPrincipal) spc.getPrimaryPrincipal();
                return p.getApplicationId().toString();
            }
        }
        return null;
    }
}
