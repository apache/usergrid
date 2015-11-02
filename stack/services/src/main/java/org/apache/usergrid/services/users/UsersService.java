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
package org.apache.usergrid.services.users;


import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.usergrid.persistence.cache.CacheScope;
import org.apache.usergrid.persistence.cache.ScopedCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.entities.Role;
import org.apache.usergrid.security.shiro.utils.SubjectUtils;
import org.apache.usergrid.services.AbstractCollectionService;
import org.apache.usergrid.services.ServiceContext;
import org.apache.usergrid.services.ServicePayload;
import org.apache.usergrid.services.ServiceRequest;
import org.apache.usergrid.services.ServiceResults;
import org.apache.usergrid.services.exceptions.ServiceResourceNotFoundException;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.usergrid.persistence.Schema.PROPERTY_EMAIL;
import static org.apache.usergrid.persistence.Schema.PROPERTY_PICTURE;
import static org.apache.usergrid.services.ServiceResults.genericServiceResults;
import static org.apache.usergrid.utils.ConversionUtils.string;


public class UsersService extends AbstractCollectionService {

    private static final Logger LOG = LoggerFactory.getLogger( UsersService.class );


    public UsersService() {
        super();
        LOG.debug( "/users" );

        makeConnectionPrivate( "following" );

        declareVirtualCollections( Arrays.asList( "following", "followers" ) );

        addReplaceParameters( Arrays.asList( "$id", "followers" ), Arrays.asList( "\\0", "connecting", "following" ) );

        // rolenames is the one case of Entity Dictionary name not equal to path segment
        declareEntityDictionary( new EntityDictionaryEntry( "rolenames", "roles" ) );

        declareEntityDictionaries( Arrays.asList( "permissions" ) );
    }


    @Override
    public ServiceResults getItemByName( ServiceContext context, String name ) throws Exception {
        String nameProperty = Schema.getDefaultSchema().aliasProperty( getEntityType() );

        if ( nameProperty == null ) {
            nameProperty = "name";
        }

        EntityRef entity = null;
        Identifier id = Identifier.from( name );

        if ( id != null ) {
            entity = em.getUserByIdentifier( id );
        }

        if ( entity == null ) {
            throw new ServiceResourceNotFoundException( context );
        }

        if ( !context.moreParameters() ) {
            entity = em.get( entity );
            entity = importEntity( context, ( Entity ) entity );
        }

        checkPermissionsForEntity( context, entity );

        List<ServiceRequest> nextRequests = context.getNextServiceRequests( entity );

        return new ServiceResults( this, context, ServiceResults.Type.COLLECTION, Results.fromRef( entity ), null,
                nextRequests );
    }


    @Override
    public ServiceResults invokeItemWithName( ServiceContext context, String name ) throws Exception {
        if ( "me".equals( name ) ) {
            UserInfo user = SubjectUtils.getUser();
            if ( ( user != null ) && ( user.getUuid() != null ) ) {
                return super.invokeItemWithId( context, user.getUuid() );
            }
        }
        return super.invokeItemWithName( context, name );
    }


    @Override
    public ServiceResults postCollection( ServiceContext context ) throws Exception {
        Iterator<Map<String, Object>> i = context.getPayload().payloadIterator();
        while ( i.hasNext() ) {
            Map<String, Object> p = i.next();
            setGravatar( p );
        }
        return super.postCollection( context );
    }


    public void setGravatar( Map<String, Object> p ) {
        if ( isBlank( string( p.get( PROPERTY_PICTURE ) ) ) && isNotBlank( string( p.get( "email" ) ) ) ) {
            p.put( PROPERTY_PICTURE, "http://www.gravatar.com/avatar/" + md5Hex(
                    string( p.get( PROPERTY_EMAIL ) ).trim().toLowerCase() ) );
        }
    }


    public ServiceResults getUserRoles( UUID userId ) throws Exception {
        Map<String, Role> roles = em.getUserRolesWithTitles( userId );
        // roles.put("default", "Default");
        ServiceResults results = genericServiceResults().withData( roles );
        return results;
    }


    public ServiceResults getApplicationRolePermissions( String roleName ) throws Exception {
        Set<String> permissions = em.getRolePermissions( roleName );
        ServiceResults results = genericServiceResults().withData( permissions );
        return results;
    }


    public ServiceResults addUserRole( UUID userId, String roleName ) throws Exception {
        em.addUserToRole( userId, roleName );
        return getUserRoles( userId );
    }


    public ServiceResults deleteUserRole( UUID userId, String roleName ) throws Exception {
        em.removeUserFromRole( userId, roleName );
        return getUserRoles( userId );
    }


    @Override
    public ServiceResults getEntityDictionary( ServiceContext context, List<EntityRef> refs,
                                               EntityDictionaryEntry dictionary ) throws Exception {

        if ( "rolenames".equalsIgnoreCase( dictionary.getName() ) ) {
            EntityRef entityRef = refs.get( 0 );
            checkPermissionsForEntitySubPath( context, entityRef, "rolenames" );

            if ( context.parameterCount() == 0 ) {

                return getUserRoles( entityRef.getUuid() );
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

            return genericServiceResults().withData( em.getUserPermissions( entityRef.getUuid() ) );
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

            em.grantUserPermission( entityRef.getUuid(), permission );
            ScopedCache scopedCache = cacheFactory.getScopedCache(new CacheScope(em.getApplication().asId()));
            scopedCache.invalidate();

            return genericServiceResults().withData( em.getUserPermissions( entityRef.getUuid() ) );
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

                return addUserRole( entityRef.getUuid(), name );
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

                return deleteUserRole( entityRef.getUuid(), roleName );
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
                em.revokeUserPermission( entityRef.getUuid(), permission );
            }

            ScopedCache scopedCache = cacheFactory.getScopedCache(new CacheScope(em.getApplication().asId()));
            scopedCache.invalidate();

            return genericServiceResults().withData( em.getUserPermissions( entityRef.getUuid() ) );
        }

        return super.deleteEntityDictionary( context, refs, dictionary );
    }
}
