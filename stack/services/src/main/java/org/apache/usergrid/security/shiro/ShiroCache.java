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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.StringJoiner;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.usergrid.persistence.cache.CacheFactory;
import org.apache.usergrid.persistence.cache.CacheScope;
import org.apache.usergrid.persistence.cache.ScopedCache;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.security.shiro.credentials.AccessTokenCredentials;
import org.apache.usergrid.security.shiro.principals.AdminUserPrincipal;
import org.apache.usergrid.security.shiro.principals.ApplicationGuestPrincipal;
import org.apache.usergrid.security.shiro.principals.ApplicationPrincipal;
import org.apache.usergrid.security.shiro.principals.ApplicationUserPrincipal;
import org.apache.usergrid.security.shiro.principals.OrganizationPrincipal;
import org.apache.usergrid.security.shiro.principals.PrincipalIdentifier;
import org.apache.usergrid.security.shiro.utils.LocalShiroCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;


/**
 * Plugin Usergrid cache for Shiro.
 */
public class ShiroCache<K, V> implements Cache<K,V> {

    private static final Logger logger = LoggerFactory.getLogger( ShiroCache.class );

    private final CacheFactory<String, V> cacheFactory;
    private final TypeReference typeRef;
    private final Integer cacheTtl;
    private final LocalShiroCache localShiroCache;

    public ShiroCache(TypeReference typeRef, CacheFactory<String, V> cacheFactory, Integer cacheTtl, LocalShiroCache<K,V> localShiroCache) {
        this.typeRef = typeRef;
        this.cacheFactory = cacheFactory;
        this.cacheTtl = cacheTtl;
        this.localShiroCache = localShiroCache;
    }

    @Override
    public V get(K key) throws CacheException {
        if ( cacheTtl == 0 ) return null;

        V value;
        String ks = getKeyString(key);
        //check cache first
        value = (V) localShiroCache.get(ks);
        if( value !=null ){
            if(logger.isTraceEnabled()) {
                logger.trace("Shiro value served from local cache: {}", value);
            }
            return value;

        }

        ScopedCache<String, V> scopedCache = getCacheScope(key);
        if ( scopedCache != null ) {

            value = scopedCache.get(ks, typeRef);

            if(value != null) {

                if(logger.isTraceEnabled()) {
                    logger.trace("Shiro value service from cassandra cache: {}", value);
                }

                localShiroCache.put(ks, value);
            }

            if ( logger.isTraceEnabled() ) {
                if (value instanceof UsergridAuthorizationInfo) {
                    UsergridAuthorizationInfo info = (UsergridAuthorizationInfo) value;
                    logger.trace("Got from AUTHZ cache {} for app {}", ks, info.toString());
                } else if (value instanceof UsergridAuthenticationInfo) {
                    UsergridAuthenticationInfo info = (UsergridAuthenticationInfo) value;
                    logger.trace("Got from AUTHC cache {} for app {}", ks, info.toString());

                } else if (value == null) {
                    logger.trace("Got NULL from cache app {} for key {}", ks, key.toString());
                }
            }

        }

        return value;
    }

    @Override
    public V put(K key, V value) throws CacheException {
        if ( cacheTtl == 0 ) return null;

        ScopedCache<String, V> scopedCache = getCacheScope(key);
        if ( scopedCache != null ) {
        	String ks = getKeyString(key);
        	
            V ret = scopedCache.put(ks, value, cacheTtl);
            localShiroCache.invalidate(ks);

            if ( logger.isTraceEnabled() ) {
                if (value instanceof UsergridAuthorizationInfo) {
                    UsergridAuthorizationInfo info = (UsergridAuthorizationInfo) value;
                    logger.trace("Put to AUTHZ cache {} for app {}", ks, info.toString());

                } else if (value instanceof UsergridAuthenticationInfo) {
                    UsergridAuthenticationInfo info = (UsergridAuthenticationInfo) value;
                    logger.trace("Put to AUTHC cache {} for app {}", ks, info.toString());
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
        String ks = getKeyString(key);
        
        if ( scopedCache != null ) {
            scopedCache.remove( ks );
        }
        
        localShiroCache.invalidate(ks);
        return null;
    }

    @Override
    public void clear() throws CacheException {
        localShiroCache.invalidateAll();
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

        String ret = null;
        Throwable throwable = null;
        String errorMessage = null;

        try {

            final String typeName = typeRef.getType().getTypeName();
            PrincipalIdentifier principalIdentifier;

            if (key instanceof SimplePrincipalCollection) {

                SimplePrincipalCollection spc = (SimplePrincipalCollection) key;

                if (spc.getPrimaryPrincipal() instanceof PrincipalIdentifier) {

                    // principal is not user, try to get something unique as cache key
                	principalIdentifier = (PrincipalIdentifier) spc.getPrimaryPrincipal();

                } else {
                    errorMessage = "Unknown principal type: " + key.getClass().getSimpleName();
                    throw new CacheException( errorMessage );
                }

            } else if (key instanceof PrincipalIdentifier) {
                principalIdentifier = (PrincipalIdentifier)key;
                
            } else {
                // not a principal identifier, don't cache
                errorMessage = "Unknown key type: " + key.getClass().getSimpleName();
                throw new CacheException(errorMessage);
            }
            
            String token = principalIdentifier != null && principalIdentifier.getAccessTokenCredentials() != null ? principalIdentifier.getAccessTokenCredentials().getToken() : null;
            
            if (principalIdentifier instanceof ApplicationGuestPrincipal) {
            	//Guest principal needs a special identifier to ensure that the key is not the same as application principal
                ApplicationGuestPrincipal agp = (ApplicationGuestPrincipal) principalIdentifier;
                ret = buildKeyString("GUEST",agp.getApplicationId().toString(), typeName, token);

            } else if (principalIdentifier instanceof ApplicationPrincipal) {
                ApplicationPrincipal ap = (ApplicationPrincipal) principalIdentifier;
                ret = buildKeyString(ap.getApplicationId().toString(), typeName, token);

            } else if (principalIdentifier instanceof OrganizationPrincipal) {
                OrganizationPrincipal op = (OrganizationPrincipal) principalIdentifier;
                ret = buildKeyString(op.getOrganizationId().toString(), typeName, token);

            } else if (principalIdentifier instanceof ApplicationUserPrincipal) {
            	ApplicationUserPrincipal apup = (ApplicationUserPrincipal) principalIdentifier;
                ret = buildKeyString(apup.getUser().getUuid().toString(), typeName, token);

            } else if (principalIdentifier instanceof AdminUserPrincipal) {
            	AdminUserPrincipal adup = (AdminUserPrincipal) principalIdentifier;
                ret = buildKeyString(adup.getUser().getUuid().toString(), typeName, token);

            } else {
                errorMessage = "Unknown key type: " + key.getClass().getSimpleName();
            }

        } catch ( Throwable t ) {
            throwable = t;
        }

        if ( throwable != null ) {
            errorMessage = "Error generating cache key for key type " + key.getClass().getSimpleName();
            throw new CacheException( errorMessage, throwable );
        }

        if ( ret == null ) {
            throw new CacheException( errorMessage );
        }

        return ret;
    }
    
    private String buildKeyString(String ... components) {
    	StringJoiner sj = new StringJoiner("_");
    	for(String component : components) {
    		if(component != null) {
    			sj.add(component);
    		}
    	}
    	return sj.toString();
    }

}
