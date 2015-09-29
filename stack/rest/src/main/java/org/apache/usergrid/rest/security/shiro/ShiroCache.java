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
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.cache.CacheFactory;
import org.apache.usergrid.persistence.cache.CacheScope;
import org.apache.usergrid.persistence.cache.ScopedCache;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.security.shiro.UsergridAuthenticationInfo;
import org.apache.usergrid.security.shiro.UsergridAuthorizationInfo;
import org.apache.usergrid.security.shiro.principals.ApplicationGuestPrincipal;
import org.apache.usergrid.security.shiro.principals.ApplicationPrincipal;
import org.apache.usergrid.security.shiro.principals.OrganizationPrincipal;
import org.apache.usergrid.security.shiro.principals.UserPrincipal;
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
        ScopedCache<String, V> scopedCache = getCacheScope(key);
        if ( scopedCache != null ) {
            V ret = scopedCache.put(getKeyString(key), value, 5000);

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

        UUID applicationId;

        if ( key instanceof SimplePrincipalCollection) {
            SimplePrincipalCollection spc = (SimplePrincipalCollection)key;

            if ( spc.getPrimaryPrincipal() instanceof UserPrincipal ) {
                UserPrincipal p = (UserPrincipal) spc.getPrimaryPrincipal();
                applicationId = p.getApplicationId();

            } else  if ( spc.getPrimaryPrincipal() instanceof ApplicationPrincipal ) {
                ApplicationPrincipal p = (ApplicationPrincipal)spc.getPrimaryPrincipal();
                applicationId = p.getApplicationId();

            } else  if ( spc.getPrimaryPrincipal() instanceof OrganizationPrincipal ) {
                applicationId = CpNamingUtils.MANAGEMENT_APPLICATION_ID;

            } else if ( spc.getPrimaryPrincipal() instanceof ApplicationGuestPrincipal) {
                ApplicationGuestPrincipal p = (ApplicationGuestPrincipal)spc.getPrimaryPrincipal();
                applicationId = p.getApplicationId();

            } else {
                logger.error("Unknown principal type: " + spc.getPrimaryPrincipal().getClass().getSimpleName());
                throw new RuntimeException("Unknown principal type: "
                    + spc.getPrimaryPrincipal().getClass().getSimpleName());
            }

        } else if ( key instanceof UserPrincipal ) {
            UserPrincipal p = (UserPrincipal)key;
            applicationId = p.getApplicationId();

        } else if ( key instanceof ApplicationPrincipal ) {
            ApplicationPrincipal p = (ApplicationPrincipal)key;
            applicationId = p.getApplicationId();

        } else if ( key instanceof OrganizationPrincipal ) {
            applicationId = CpNamingUtils.MANAGEMENT_APPLICATION_ID;

        } else if ( key instanceof ApplicationGuestPrincipal) {
            ApplicationGuestPrincipal p = (ApplicationGuestPrincipal)key;
            applicationId = p.getApplicationId();

        } else {
            logger.error("Unknown key type: " + key.getClass().getSimpleName());
            throw new RuntimeException("Unknown key type: " + key.getClass().getSimpleName());
        }

        CacheScope scope = new CacheScope(new SimpleId(applicationId, "application"));
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

    public void invalidate( UUID applicationId ) {
        CacheScope scope = new CacheScope( new SimpleId(applicationId, "application") );
        ScopedCache cache = cacheFactory.getScopedCache(scope);
        cache.invalidate();
    }
}
