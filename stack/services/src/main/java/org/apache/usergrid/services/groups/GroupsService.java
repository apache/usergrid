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
package org.apache.usergrid.services.groups;


import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.usergrid.persistence.cache.CacheScope;
import org.apache.usergrid.persistence.cache.ScopedCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.entities.Role;
import org.apache.usergrid.services.AbstractPathBasedColllectionService;
import org.apache.usergrid.services.ServiceContext;
import org.apache.usergrid.services.ServicePayload;
import org.apache.usergrid.services.ServiceResults;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.usergrid.services.ServiceResults.genericServiceResults;


public class GroupsService extends AbstractPathBasedColllectionService {

    private static final Logger logger = LoggerFactory.getLogger( GroupsService.class );

    static CharMatcher matcher = CharMatcher.JAVA_LETTER_OR_DIGIT.or( CharMatcher.anyOf( "-./" ) );


    public GroupsService() {
        super();
        logger.debug( "/groups" );

        // rolenames is the one case of Entity Dictionary name not equal to path segment
        declareEntityDictionary( new EntityDictionaryEntry( "rolenames", "roles" ) );

        declareEntityDictionaries( Arrays.asList( "permissions" ) );
    }


    @Override
    public ServiceResults postCollection( ServiceContext context ) throws Exception {

        String path = ( String ) context.getProperty( "path" );

        if ( path == null ) {
            throw new IllegalArgumentException( "You must provide a 'path' property when creating a group" );
        }

        logger.debug( "Creating group with path {}", path );

        Preconditions.checkArgument( matcher.matchesAllOf( path ), "Illegal characters found in group name: " + path );

        return super.postCollection( context );
    }


    public ServiceResults getGroupRoles( UUID groupId ) throws Exception {
        Map<String, Role> roles = em.getGroupRolesWithTitles( groupId );
        ServiceResults results = genericServiceResults().withData( roles );
        return results;
    }


    public ServiceResults getApplicationRolePermissions( String roleName ) throws Exception {
        Set<String> permissions = em.getRolePermissions( roleName );
        ServiceResults results = genericServiceResults().withData( permissions );
        return results;
    }


    public ServiceResults addGroupRole( UUID groupId, String roleName ) throws Exception {
        em.addGroupToRole( groupId, roleName );
        ScopedCache scopedCache = cacheFactory.getScopedCache(new CacheScope(em.getApplication().asId()));
        scopedCache.invalidate();
        return getGroupRoles( groupId );
    }


    public ServiceResults deleteGroupRole( UUID groupId, String roleName ) throws Exception {
        em.removeGroupFromRole( groupId, roleName );
        ScopedCache scopedCache = cacheFactory.getScopedCache(new CacheScope(em.getApplication().asId()));
        scopedCache.invalidate();
        return getGroupRoles( groupId );
    }


    @Override
    public ServiceResults getEntityDictionary( ServiceContext context, List<EntityRef> refs,
                                               EntityDictionaryEntry dictionary ) throws Exception {

        if ( "rolenames".equalsIgnoreCase( dictionary.getName() ) ) {
            EntityRef entityRef = refs.get( 0 );
            checkPermissionsForEntitySubPath( context, entityRef, "rolenames" );

            if ( context.parameterCount() == 0 ) {

                return getGroupRoles( entityRef.getUuid() );
            }
            else if ( context.parameterCount() == 1 ) {

                String roleName = context.getParameters().get( 1 ).getName();
                if ( isBlank( roleName ) ) {
                    return null;
                }

                return getApplicationRolePermissions( roleName );
            }
        }
        else if ( "permissions".equalsIgnoreCase( dictionary.getName() ) ) {
            EntityRef entityRef = refs.get( 0 );
            checkPermissionsForEntitySubPath( context, entityRef, "permissions" );

            return genericServiceResults().withData( em.getGroupPermissions( entityRef.getUuid() ) );
        }

        return super.getEntityDictionary( context, refs, dictionary );
    }


    @Override
    public ServiceResults postEntityDictionary( ServiceContext context, List<EntityRef> refs,
                                                EntityDictionaryEntry dictionary, ServicePayload payload )
            throws Exception {

        if ( "permissions".equalsIgnoreCase( dictionary.getName() ) ) {
            EntityRef entityRef = refs.get( 0 );
            checkPermissionsForEntitySubPath( context, entityRef, "permissions" );

            String permission = payload.getStringProperty( "permission" );
            if ( isBlank( permission ) ) {
                return null;
            }

            em.grantGroupPermission( entityRef.getUuid(), permission );
            ScopedCache scopedCache = cacheFactory.getScopedCache(new CacheScope(em.getApplication().asId()));
            scopedCache.invalidate();

            return genericServiceResults().withData( em.getGroupPermissions( entityRef.getUuid() ) );
        }

        return super.postEntityDictionary( context, refs, dictionary, payload );
    }


    @Override
    public ServiceResults putEntityDictionary( ServiceContext context, List<EntityRef> refs,
                                               EntityDictionaryEntry dictionary, ServicePayload payload )
            throws Exception {

        if ( "rolenames".equalsIgnoreCase( dictionary.getName() ) ) {
            EntityRef entityRef = refs.get( 0 );
            checkPermissionsForEntitySubPath( context, entityRef, "rolenames" );

            if ( context.parameterCount() == 0 ) {

                String name = payload.getStringProperty( "name" );
                if ( isBlank( name ) ) {
                    return null;
                }

                return addGroupRole( entityRef.getUuid(), name );
            }
        }

        return super.postEntityDictionary( context, refs, dictionary, payload );
    }


    @Override
    public ServiceResults deleteEntityDictionary( ServiceContext context, List<EntityRef> refs,
                                                  EntityDictionaryEntry dictionary ) throws Exception {

        if ( "rolenames".equalsIgnoreCase( dictionary.getName() ) ) {
            EntityRef entityRef = refs.get( 0 );
            checkPermissionsForEntitySubPath( context, entityRef, "rolenames" );

            if ( context.parameterCount() == 1 ) {

                String roleName = context.getParameters().get( 1 ).getName();
                if ( isBlank( roleName ) ) {
                    return null;
                }

                return deleteGroupRole( entityRef.getUuid(), roleName );
            }
        }
        else if ( "permissions".equalsIgnoreCase( dictionary.getName() ) ) {
            EntityRef entityRef = refs.get( 0 );
            checkPermissionsForEntitySubPath( context, entityRef, "permissions" );

            Query q = context.getParameters().get( 0 ).getQuery();
            if ( q == null ) {
                return null;
            }

            List<String> permissions = q.getPermissions();
            if ( permissions == null ) {
                return null;
            }

            for ( String permission : permissions ) {
                em.revokeGroupPermission( entityRef.getUuid(), permission );
            }
            ScopedCache scopedCache = cacheFactory.getScopedCache(new CacheScope(em.getApplication().asId()));
            scopedCache.invalidate();

            return genericServiceResults().withData( em.getGroupPermissions( entityRef.getUuid() ) );
        }

        return super.deleteEntityDictionary( context, refs, dictionary );
    }
}
