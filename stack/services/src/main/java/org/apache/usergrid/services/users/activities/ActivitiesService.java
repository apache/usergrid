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
package org.apache.usergrid.services.users.activities;


import java.util.*;

import org.apache.usergrid.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.entities.Activity;
import org.apache.usergrid.persistence.entities.Activity.ActivityObject;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.Query.Level;
import org.apache.usergrid.services.ServiceContext;
import org.apache.usergrid.services.ServicePayload;
import org.apache.usergrid.services.ServiceResults;
import org.apache.usergrid.services.generic.GenericCollectionService;


public class ActivitiesService extends GenericCollectionService {

    private static final Logger logger = LoggerFactory.getLogger( ActivitiesService.class );


    public ActivitiesService() {
        super();
        logger.debug( "/users/*/activities" );
    }


    @SuppressWarnings("unchecked")
    @Override
    public ServiceResults postCollection( ServiceContext context ) throws Exception {

        ServicePayload payload = context.getPayload();

        Entity user = em.get( context.getOwner() );

        Object actor = payload.getProperty( Activity.PROPERTY_ACTOR );

        if ( actor instanceof Map ) {
            handleDynamicPayload( ( Map<String, String> ) actor, user, payload );
        }
        else if ( actor instanceof ActivityObject ) {
            handleDynamicPayload( ( ActivityObject ) actor, user, payload );
        }
        else if ( actor == null ) {
            handleDynamicPayload( ( ActivityObject ) actor, user, payload );
        }

        ServiceResults results = super.postCollection( context );

        distribute( context.getOwner(), results.getEntity() );
        return results;
    }


    /** Invoked when our actor is a map */
    private void handleDynamicPayload( Map<String, String> actor, Entity user, ServicePayload payload ) {

        // create a new actor object
        if ( actor == null ) {
            actor = new HashMap<String, String>();
            payload.setProperty( Activity.PROPERTY_ACTOR, actor );
        }

        if ( user != null ) {
            if ( actor.get( User.PROPERTY_UUID ) == null && user.getUuid() != null ) {
                actor.put( User.PROPERTY_UUID, user.getUuid().toString() );
            }

            if ( actor.get( User.PROPERTY_EMAIL ) == null && user.getProperty( User.PROPERTY_EMAIL ) != null ) {
                actor.put( User.PROPERTY_EMAIL, user.getProperty( User.PROPERTY_EMAIL ).toString() );
            }
        }
    }


    /** Invoked to set values when our actor is an activity object */
    private void handleDynamicPayload( ActivityObject actor, Entity user, ServicePayload payload ) {

        // create a new actor object
        if ( actor == null ) {
            actor = new ActivityObject();
            payload.setProperty( Activity.PROPERTY_ACTOR, actor );
        }

        if ( user != null ) {
            if ( actor.getId() == null && user.getUuid() != null ) {
                actor.setUuid( user.getUuid() );
                // TODO TN should this also populate id?
            }

            if ( actor.getDynamicProperties().get( User.PROPERTY_EMAIL ) == null
                    && user.getProperty( User.PROPERTY_EMAIL ) != null ) {
                actor.getDynamicProperties()
                     .put( User.PROPERTY_EMAIL, user.getProperty( User.PROPERTY_EMAIL ).toString() );
            }
        }
    }


    @Override
    public ServiceResults postItemById( ServiceContext context, UUID id ) throws Exception {

        ServiceResults results = super.postItemById( context, id );

        distribute( context.getOwner(), results.getEntity() );
        return results;
    }


    public void distribute( EntityRef user, Entity activity ) throws Exception {
        if ( activity == null ) {
            return;
        }
        //add activity
        em.addToCollection( user, "feed", activity );

        //publish to all connections
        Results results =  em.getSourceEntities(
            new SimpleEntityRef(user.getType(), user.getUuid()),
            "following", User.ENTITY_TYPE, Level.REFS);

        if( results != null ){
            PagingResultsIterator itr = new PagingResultsIterator(results);

            List<EntityRef> refs = new ArrayList<EntityRef>();
            EntityRef c;
            int breaker = 10000;
            //collect
            while (itr.hasNext()) {
                c = (EntityRef) itr.next();
                refs.add(c);
                //break out when you get too big
                if( refs.size() > breaker ){
                    em.addToCollections(refs, "feed", activity);
                    refs.clear();
                }
            }
            //add to collections
            if (refs.size() > 0) {
                em.addToCollections(refs, "feed", activity);
            }
        }
    }
}
