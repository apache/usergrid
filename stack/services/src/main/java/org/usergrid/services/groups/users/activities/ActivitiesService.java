/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Stack.
 * 
 * Usergrid Stack is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * 
 * Usergrid Stack is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License along
 * with Usergrid Stack. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU AGPL version 3 section 7
 * 
 * Linking Usergrid Stack statically or dynamically with other modules is making
 * a combined work based on Usergrid Stack. Thus, the terms and conditions of the
 * GNU General Public License cover the whole combination.
 * 
 * In addition, as a special exception, the copyright holders of Usergrid Stack
 * give you permission to combine Usergrid Stack with free software programs or
 * libraries that are released under the GNU LGPL and with independent modules
 * that communicate with Usergrid Stack solely through:
 * 
 *   - Classes implementing the org.usergrid.services.Service interface
 *   - Apache Shiro Realms and Filters
 *   - Servlet Filters and JAX-RS/Jersey Filters
 * 
 * You may copy and distribute such a system following the terms of the GNU AGPL
 * for Usergrid Stack and the licenses of the other code concerned, provided that
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
