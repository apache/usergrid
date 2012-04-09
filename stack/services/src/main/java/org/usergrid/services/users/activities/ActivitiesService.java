/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.services.users.activities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.entities.Activity;
import org.usergrid.persistence.entities.User;
import org.usergrid.services.ServiceContext;
import org.usergrid.services.ServicePayload;
import org.usergrid.services.ServiceResults;
import org.usergrid.services.generic.GenericCollectionService;

public class ActivitiesService extends GenericCollectionService {

    private static final Logger logger = LoggerFactory
            .getLogger(ActivitiesService.class);

    public ActivitiesService() {
        super();
        logger.info("/users/*/activities");
    }

    @Override
    public ServiceResults postCollection(ServiceContext context)
            throws Exception {

        // TODO Todd Nine. context.getPayload(), add the default actor with both
        // id and
        // email. Prefix the id with "usergrid". Both can be null so be null
        // safe

        ServicePayload payload = context.getPayload();

        @SuppressWarnings("unchecked")
        Map<String, String> actor = (Map<String, String>) payload
                .getProperty(Activity.PROP_ACTOR);

        Entity user = em.get(context.getOwner());

        // create a new actor object
        if (actor == null) {
            actor = new HashMap<String, String>();
            payload.setProperty(Activity.PROP_ACTOR, actor);
        }

        if (user != null) {
            if (actor.get(User.PROP_UUID) == null && user.getUuid() != null) {
                actor.put(User.PROP_UUID, user.getUuid().toString());
            }

            if (actor.get(User.PROP_EMAIL) == null && user.getProperty(User.PROP_EMAIL) != null) {
                actor.put(User.PROP_EMAIL, user.getProperty(User.PROP_EMAIL).toString());
            }
        }

        ServiceResults results = super.postCollection(context);

        distribute(context.getOwner(), results.getEntity());
        return results;
    }

    @Override
    public ServiceResults postItemById(ServiceContext context, UUID id)
            throws Exception {

        ServiceResults results = super.postItemById(context, id);

        distribute(context.getOwner(), results.getEntity());
        return results;
    }

    public void distribute(EntityRef user, Entity activity) throws Exception {
        if (activity == null) {
            return;
        }
        em.addToCollection(user, "feed", activity);
        Results r = em.getConnectingEntities(user.getUuid(), "following",
                User.ENTITY_TYPE, Results.Level.REFS);
        List<EntityRef> refs = r.getRefs();
        if (refs != null) {
            em.addToCollections(refs, "feed", activity);
        }
    }

}
