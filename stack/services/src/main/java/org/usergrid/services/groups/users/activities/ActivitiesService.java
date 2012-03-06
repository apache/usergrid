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
package org.usergrid.services.groups.users.activities;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.entities.User;
import org.usergrid.services.ServiceContext;
import org.usergrid.services.ServiceResults;
import org.usergrid.services.generic.GenericCollectionService;

public class ActivitiesService extends GenericCollectionService {

	private static final Logger logger = LoggerFactory
			.getLogger(ActivitiesService.class);

	public ActivitiesService() {
		super();
		logger.info("/groups/*/users/*/activities");
	}

	@Override
	public ServiceResults postCollection(ServiceContext context)
			throws Exception {

		ServiceResults results = super.postCollection(context);

		distribute(context.getPreviousResults().getRef(), context.getOwner(),
				results.getEntity());
		return results;
	}

	@Override
	public ServiceResults postItemById(ServiceContext context, UUID id)
			throws Exception {

		ServiceResults results = super.postItemById(context, id);

		distribute(context.getPreviousResults().getRef(), context.getOwner(),
				results.getEntity());
		return results;
	}

	public void distribute(EntityRef group, EntityRef user, Entity activity)
			throws Exception {
		if (activity == null) {
			return;
		}
		em.addToCollection(user, "feed", activity);
		Results r1 = em.getCollection(group, "users", null, 10000,
				Results.Level.IDS, false);
		if ((r1 == null) || (r1.isEmpty())) {
			return;
		}
		Results r2 = em.getConnectingEntities(user.getUuid(), "following",
				User.ENTITY_TYPE, Results.Level.IDS);

		if ((r2 == null) || (r2.isEmpty())) {
			return;
		}
		r1.and(r2);
		List<EntityRef> refs = Results
				.fromIdList(r1.getIds(), User.ENTITY_TYPE).getRefs();
		if (refs != null) {
			em.addToCollections(refs, "feed", activity);
		}
	}
}
