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
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.usergrid.persistence.cache.CacheFactory;
import org.apache.usergrid.persistence.cache.CacheScope;
import org.apache.usergrid.persistence.cache.ScopedCache;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.security.shiro.UsergridAuthenticationInfo;
import org.apache.usergrid.security.shiro.UsergridAuthorizationInfo;
import org.apache.usergrid.security.shiro.principals.AdminUserPrincipal;
import org.apache.usergrid.security.shiro.principals.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * Plugin Usergrid cache for Shiro.
 */
public class ShiroCache<K, V> implements Cache<K,V> {

    private static final Logger logger = LoggerFactory.getLogger( ShiroCacheManager.class );

    CacheFactory<String, V> cacheFactory;

    TypeReference typeRef = null;


    public ShiroCache( TypeReference typeRef, CacheFactory<String, V> cacheFactory ) {
        this.typeRef = typeRef;
        this.cacheFactory = cacheFactory;
    }

    @Override
    public V get(K key) throws CacheException {
        ScopedCache<String, V> scopedCache = getCacheScope(key);
        if ( scopedCache != null ) {
            V value = scopedCache.get(getKeyString(key), typeRef);

            if ( value instanceof UsergridAuthorizationInfo ) {
                UsergridAuthorizationInfo info = (UsergridAuthorizationInfo)value;
                logger.debug("Got from AUTHZ cache {} for app {}", getKeyString(key), info.toString());

            } else if ( value instanceof UsergridAuthenticationInfo ) {
                UsergridAuthenticationInfo info = (UsergridAuthenticationInfo)value;
                logger.debug("Got from AUTHC cache {} for app {}", getKeyString(key), info.toString());

            } else if (value == null) {
                logger.debug("Got NULL from cache app {} for key {}", getKeyString(key), key.toString() );
            }

            return value;
        }
        return null;
    }

    @Override
    public V put(K key, V value) throws CacheException {
        ScopedCache<String, V> scopedCache = getCacheScope(key);
        if ( scopedCache != null ) {
            V ret = scopedCache.put(getKeyString(key), value, 5000);

            if ( value instanceof UsergridAuthorizationInfo ) {
                UsergridAuthorizationInfo info = (UsergridAuthorizationInfo)value;
                logger.debug("Put to AUTHZ cache {} for app {}", getKeyString(key), info.toString());

            } else if ( value instanceof UsergridAuthenticationInfo ) {
                UsergridAuthenticationInfo info = (UsergridAuthenticationInfo)value;
                logger.debug("Put to AUTHC cache {} for app {}", getKeyString(key), info.toString());
            }

            return ret;
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

            } else {
                throw new RuntimeException("Cannot determine application ID for cache scope");
            }

        } else if ( key instanceof AdminUserPrincipal ) {

            AdminUserPrincipal p = (AdminUserPrincipal)key;
            CacheScope scope = new CacheScope(new SimpleId(p.getApplicationId(), "application"));
            ScopedCache<String, V> scopedCache = cacheFactory.getScopedCache(scope);
            return scopedCache;

        } else {
            throw new RuntimeException("Cannot determine application ID for cache scope");
        }
    }


    /** key is the user UUID in string form */
    private String getKeyString( K key ) {

        if ( key instanceof SimplePrincipalCollection) {
            SimplePrincipalCollection spc = (SimplePrincipalCollection)key;

            if ( spc.getPrimaryPrincipal() instanceof UserPrincipal) {
                AdminUserPrincipal p = (AdminUserPrincipal) spc.getPrimaryPrincipal();
                return p.getUser().getUuid().toString();
            }
        }

        return key.toString();
    }

    public void invalidate( UUID applicationId ) {
        CacheScope scope = new CacheScope( new SimpleId(applicationId, "application") );
        ScopedCache cache = cacheFactory.getScopedCache(scope);
        cache.invalidate();

    }
}
