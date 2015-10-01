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
package org.apache.usergrid.services.groups.users.activities;


import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.Query.Level;
import org.apache.usergrid.services.ServiceContext;
import org.apache.usergrid.services.ServiceResults;
import org.apache.usergrid.services.generic.GenericCollectionService;


public class ActivitiesService extends GenericCollectionService {

    private static final Logger logger = LoggerFactory.getLogger( ActivitiesService.class );


    public ActivitiesService() {
        super();
        logger.debug( "/groups/*/users/*/activities" );
    }


    @Override
    public ServiceResults postCollection( ServiceContext context ) throws Exception {

        ServiceResults results = super.postCollection( context );

        distribute( context.getPreviousResults().getRef(), context.getOwner(), results.getEntity() );
        return results;
    }


    @Override
    public ServiceResults postItemById( ServiceContext context, UUID id ) throws Exception {

        ServiceResults results = super.postItemById( context, id );

        distribute( context.getPreviousResults().getRef(), context.getOwner(), results.getEntity() );
        return results;
    }


    public void distribute( EntityRef group, EntityRef user, Entity activity ) throws Exception {
        if ( activity == null ) {
            return;
        }
        em.addToCollection( user, "feed", activity );
        Results r1 = em.getCollection( group, "users", null, 10000, Level.IDS, false );
        if ( ( r1 == null ) || ( r1.isEmpty() ) ) {
            return;
        }

        Results r2 = em.getSourceEntities(new SimpleEntityRef(user.getType(), user.getUuid()),
            "following", User.ENTITY_TYPE, Level.IDS);

        if ( ( r2 == null ) || ( r2.isEmpty() ) ) {
            return;
        }
        r1.and( r2 );
        List<EntityRef> refs = Results.fromIdList( r1.getIds(), User.ENTITY_TYPE ).getRefs();
        if ( refs != null ) {
            em.addToCollections( refs, "feed", activity );
        }
    }
}
