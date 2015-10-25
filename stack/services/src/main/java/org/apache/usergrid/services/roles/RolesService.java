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
package org.apache.usergrid.services.roles;


import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.usergrid.persistence.cache.CacheScope;
import org.apache.usergrid.persistence.cache.ScopedCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.entities.Group;
import org.apache.usergrid.services.AbstractCollectionService;
import org.apache.usergrid.services.ServiceContext;
import org.apache.usergrid.services.ServicePayload;
import org.apache.usergrid.services.ServiceResults;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.usergrid.services.ServiceResults.genericServiceResults;


public class RolesService extends AbstractCollectionService {

    private static final Logger logger = LoggerFactory.getLogger( RolesService.class );


    public RolesService() {
        super();
        logger.debug( "/roles" );

        declareEntityDictionary( "permissions" );
    }


    @Override
    public ServiceResults getItemByName( ServiceContext context, String name ) throws Exception {
        if ( ( context.getOwner() != null ) && Group.ENTITY_TYPE.equals( context.getOwner().getType() ) ) {
            return getItemById( context, em.getGroupRoleRef( context.getOwner().getUuid(), name ).getUuid() );
        }
        return super.getItemByName( context, name );
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.usergrid.services.AbstractService#getEntityDictionary(org.apache.usergrid
     * .services.ServiceContext, java.util.List, java.lang.String)
     */
    @Override
    public ServiceResults getEntityDictionary( ServiceContext context, List<EntityRef> refs,
                                               EntityDictionaryEntry dictionary ) throws Exception {

        if ( "permissions".equalsIgnoreCase( dictionary.getName() ) ) {
            EntityRef ref = refs.get( 0 );

            checkPermissionsForEntitySubPath( context, ref, "/permissions" );

            String roleName = ( String ) em.getProperty( ref, "name" );

            //Should never happen
            if ( isBlank( roleName ) ) {
                throw new IllegalArgumentException( "You must provide a role name" );
            }

            return getApplicationRolePermissions( roleName );
        }

        return super.getEntityDictionary( context, refs, dictionary );
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.usergrid.services.AbstractService#putEntityDictionary(org.apache.usergrid
     * .services.ServiceContext, java.util.List, java.lang.String,
     * org.apache.usergrid.services.ServicePayload)
     */
    @Override
    public ServiceResults putEntityDictionary( ServiceContext context, List<EntityRef> refs,
                                               EntityDictionaryEntry dictionary, ServicePayload payload )
            throws Exception {
        return postEntityDictionary( context, refs, dictionary, payload );
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.usergrid.services.AbstractService#postEntityDictionary(org.apache.usergrid
     * .services.ServiceContext, java.util.List, java.lang.String,
     * org.apache.usergrid.services.ServicePayload)
     */
    @Override
    public ServiceResults postEntityDictionary( ServiceContext context, List<EntityRef> refs,
                                                EntityDictionaryEntry dictionary, ServicePayload payload )
            throws Exception {

        if ( "permissions".equalsIgnoreCase( dictionary.getName() ) ) {

            EntityRef ref = refs.get( 0 );

            checkPermissionsForEntitySubPath( context, ref, "/permissions" );

            String roleName = ( String ) em.getProperty( ref, "name" );

            if ( isBlank( roleName ) ) {
                throw new IllegalArgumentException(
                        String.format( "Could not load role with id '%s'", ref.getUuid() ) );
            }

            String permission = payload.getStringProperty( "permission" );

            if ( isBlank( permission ) ) {
                throw new IllegalArgumentException( "You must supply a 'permission' property" );
            }

            return grantApplicationRolePermission( roleName, permission );
        }

        return super.postEntityDictionary( context, refs, dictionary, payload );
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.usergrid.services.AbstractService#deleteEntityDictionary(org.apache.usergrid
     * .services.ServiceContext, java.util.List, java.lang.String)
     */
    @Override
    public ServiceResults deleteEntityDictionary( ServiceContext context, List<EntityRef> refs,
                                                  EntityDictionaryEntry dictionary ) throws Exception {

        if ( "permissions".equalsIgnoreCase( dictionary.getName() ) ) {


            EntityRef ref = refs.get( 0 );

            checkPermissionsForEntitySubPath( context, ref, "/permissions" );

            String roleName = ( String ) em.getProperty( ref, "name" );

            if ( isBlank( roleName ) ) {
                throw new IllegalArgumentException(
                        String.format( "Could not load role with id '%s'", ref.getUuid() ) );
            }

            Query q = null;

            if ( context.getParameters().size() > 0 ) {
                q = context.getParameters().get( 0 ).getQuery();
            }

            if ( q == null ) {
                throw new IllegalArgumentException( "You must supply a 'permission' query parameter" );
            }

            List<String> permissions = q.getPermissions();
            if ( permissions == null ) {
                throw new IllegalArgumentException( "You must supply a 'permission' query parameter" );
            }

            ServiceResults results = null;

            for ( String permission : permissions ) {

                results = revokeApplicationRolePermission( roleName, permission );
            }

            return results;
        }

        return super.deleteEntityDictionary( context, refs, dictionary );
    }


    public ServiceResults getApplicationRolePermissions( String roleName ) throws Exception {
        Set<String> permissions = em.getRolePermissions( roleName );
        ServiceResults results = genericServiceResults().withData( permissions );
        return results;
    }


    public ServiceResults grantApplicationRolePermission( String roleName, String permission ) throws Exception {
        em.grantRolePermission(roleName, permission);
        ScopedCache scopedCache = cacheFactory.getScopedCache(new CacheScope(em.getApplication().asId()));
        scopedCache.invalidate();
        return getApplicationRolePermissions( roleName );
    }


    public ServiceResults revokeApplicationRolePermission( String roleName, String permission ) throws Exception {
        em.revokeRolePermission( roleName, permission );
        ScopedCache scopedCache = cacheFactory.getScopedCache(new CacheScope(em.getApplication().asId()));
        scopedCache.invalidate();
        return getApplicationRolePermissions( roleName );
    }


    public ServiceResults getApplicationRoles() throws Exception {
        Map<String, String> roles = em.getRoles();
        ServiceResults results = genericServiceResults().withData( roles );
        return results;
    }
}
