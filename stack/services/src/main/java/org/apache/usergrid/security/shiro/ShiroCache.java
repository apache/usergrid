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
package org.apache.usergrid.security.shiro;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.cache.CacheFactory;
import org.apache.usergrid.persistence.cache.CacheScope;
import org.apache.usergrid.persistence.cache.ScopedCache;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.security.shiro.principals.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;


/**
 * Plugin Usergrid cache for Shiro.
 */
public class ShiroCache<K, V> implements Cache<K,V> {

    private static final Logger logger = LoggerFactory.getLogger( ShiroCache.class );

    private final CacheFactory<String, V> cacheFactory;
    private final TypeReference typeRef;
    private final Integer cacheTtl;

    public ShiroCache( TypeReference typeRef, CacheFactory<String, V> cacheFactory, Integer cacheTtl ) {
        this.typeRef = typeRef;
        this.cacheFactory = cacheFactory;
        this.cacheTtl = cacheTtl;
    }

    @Override
    public V get(K key) throws CacheException {
        if ( cacheTtl == 0 ) return null;

        ScopedCache<String, V> scopedCache = getCacheScope(key);
        if ( scopedCache != null ) {
            V value = scopedCache.get(getKeyString(key), typeRef);

            if ( logger.isDebugEnabled() ) {
                if (value instanceof UsergridAuthorizationInfo) {
                    UsergridAuthorizationInfo info = (UsergridAuthorizationInfo) value;
                    logger.debug("Got from AUTHZ cache {} for app {}", getKeyString(key), info.toString());

                } else if (value instanceof UsergridAuthenticationInfo) {
                    UsergridAuthenticationInfo info = (UsergridAuthenticationInfo) value;
                    logger.debug("Got from AUTHC cache {} for app {}", getKeyString(key), info.toString());

                } else if (value == null) {
                    logger.debug("Got NULL from cache app {} for key {}", getKeyString(key), key.toString());
                }
            }

            return value;
        }
        return null;
    }

    @Override
    public V put(K key, V value) throws CacheException {
        if ( cacheTtl == 0 ) return null;

        ScopedCache<String, V> scopedCache = getCacheScope(key);
        if ( scopedCache != null ) {
            V ret = scopedCache.put(getKeyString(key), value, cacheTtl);

            if ( logger.isDebugEnabled() ) {
                if (value instanceof UsergridAuthorizationInfo) {
                    UsergridAuthorizationInfo info = (UsergridAuthorizationInfo) value;
                    logger.debug("Put to AUTHZ cache {} for app {}", getKeyString(key), info.toString());

                } else if (value instanceof UsergridAuthenticationInfo) {
                    UsergridAuthenticationInfo info = (UsergridAuthenticationInfo) value;
                    logger.debug("Put to AUTHC cache {} for app {}", getKeyString(key), info.toString());
                }
            }

            return ret;
        }
        return null;
    }

    @Override
    public V remove(K key) throws CacheException {
        if ( cacheTtl == 0 ) return null;

        ScopedCache<String, V> scopedCache = getCacheScope(key);
        if ( scopedCache != null ) {
            scopedCache.remove( getKeyString(key) );
        }
        return null;
    }

    @Override
    public void clear() throws CacheException {
        // no-op: Usergrid logic will invalidate cache as necessary
    }

    @Override
    public int size() {
        return 0; // TODO?
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

        PrincipalIdentifier principal;
        if ( key instanceof SimplePrincipalCollection) {
            SimplePrincipalCollection spc = (SimplePrincipalCollection) key;
            principal = (PrincipalIdentifier) spc.getPrimaryPrincipal();

        } else {
            principal = (PrincipalIdentifier)key;
        }

        CacheScope scope = new CacheScope(new SimpleId(principal.getApplicationId(), "application"));
        ScopedCache<String, V> scopedCache = cacheFactory.getScopedCache(scope);
        return scopedCache;
    }


    /** key is the user UUID in string form + class name of key */
    private String getKeyString( K key ) {

        if ( key instanceof SimplePrincipalCollection) {
            SimplePrincipalCollection spc = (SimplePrincipalCollection)key;

            if ( spc.getPrimaryPrincipal() instanceof UserPrincipal) {
                UserPrincipal p = (UserPrincipal) spc.getPrimaryPrincipal();
                return p.getUser().getUuid().toString();
            }
        }

        return key.toString() + "_" + key.getClass().getSimpleName();
    }

}
