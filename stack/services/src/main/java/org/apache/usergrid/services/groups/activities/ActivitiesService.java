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
package org.apache.usergrid.services.groups.activities;


import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.Query.Level;
import org.apache.usergrid.services.ServiceContext;
import org.apache.usergrid.services.ServiceResults;
import org.apache.usergrid.services.generic.GenericCollectionService;


public class ActivitiesService extends GenericCollectionService {

    private static final Logger logger = LoggerFactory.getLogger( ActivitiesService.class );


    public ActivitiesService() {
        super();
        logger.debug( "/groups/*/activities" );
    }


    @Override
    public ServiceResults postCollection( ServiceContext context ) throws Exception {

        ServiceResults results = super.postCollection( context );

        distribute( context.getOwner(), results.getEntity() );
        return results;
    }


    @Override
    public ServiceResults postItemById( ServiceContext context, UUID id ) throws Exception {

        ServiceResults results = super.postItemById( context, id );

        distribute( context.getOwner(), results.getEntity() );
        return results;
    }


    public void distribute( EntityRef group, Entity activity ) throws Exception {
        if ( activity == null ) {
            return;
        }
        em.addToCollection( group, "feed", activity );
        Results r = em.getCollection( group, "users", null, 10000, Level.REFS, false );
        List<EntityRef> refs = r.getRefs();
        if ( refs != null ) {
            em.addToCollections( refs, "feed", activity );
        }
    }
}
