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
package org.apache.usergrid.services.users.roles;


import java.util.UUID;

import org.apache.usergrid.persistence.cache.CacheScope;
import org.apache.usergrid.persistence.cache.ScopedCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.services.ServiceContext;
import org.apache.usergrid.services.ServiceResults;
import org.apache.usergrid.services.ServiceResults.Type;


public class RolesService extends org.apache.usergrid.services.roles.RolesService {

    private static final Logger logger = LoggerFactory.getLogger( RolesService.class );


    public RolesService() {
        super();
        logger.info( "/users/*/roles" );
    }


    @Override
    public ServiceResults postItemById( ServiceContext context, UUID id ) throws Exception {
        User user = em.get( context.getOwner(), User.class );
        Entity entity = sm.getService( "/roles" ).getEntity( context.getRequest(), id );
        if ( entity != null ) {
            em.addUserToRole( user.getUuid(), entity.getName() );
            ScopedCache scopedCache = cacheFactory.getScopedCache(new CacheScope(em.getApplication().asId()));
            scopedCache.invalidate();
        }
        return new ServiceResults( this, context, Type.COLLECTION, Results.fromRef( entity ), null, null );
    }


    @Override
    public ServiceResults postItemByName( ServiceContext context, String name ) throws Exception {
        User user = em.get( context.getOwner(), User.class );
        Entity entity = sm.getService( "/roles" ).getEntity( context.getRequest(), name );
        if ( entity != null ) {
            em.addUserToRole( user.getUuid(), entity.getName() );
            ScopedCache scopedCache = cacheFactory.getScopedCache(new CacheScope(em.getApplication().asId()));
            scopedCache.invalidate();
        }
        return new ServiceResults( this, context, Type.COLLECTION, Results.fromRef( entity ), null, null );
    }


    @Override
    public ServiceResults deleteItemById( ServiceContext context, UUID id ) throws Exception {
        User user = em.get( context.getOwner(), User.class );
        ServiceResults results = getItemById( context, id );
        if ( !results.isEmpty() ) {
            em.removeUserFromRole( user.getUuid(), results.getEntity().getName() );
            ScopedCache scopedCache = cacheFactory.getScopedCache(new CacheScope(em.getApplication().asId()));
            scopedCache.invalidate();
        }
        return results;
    }


    @Override
    public ServiceResults deleteItemByName( ServiceContext context, String name ) throws Exception {
        User user = em.get( context.getOwner(), User.class );
        ServiceResults results = getItemByName( context, name );
        if ( !results.isEmpty() ) {
            em.removeUserFromRole( user.getUuid(), results.getEntity().getName() );
            ScopedCache scopedCache = cacheFactory.getScopedCache(new CacheScope(em.getApplication().asId()));
            scopedCache.invalidate();
        }
        return results;
    }
}
