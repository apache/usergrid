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
package org.usergrid.services.roles.users;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.entities.Role;
import org.usergrid.services.ServiceContext;
import org.usergrid.services.ServiceResults;
import org.usergrid.services.ServiceResults.Type;

public class UsersService extends org.usergrid.services.users.UsersService {

	private static final Logger logger = LoggerFactory
			.getLogger(UsersService.class);

	public UsersService() {
		super();
		logger.info("/roles/*/users");
	}

	@Override
	public ServiceResults postItemById(ServiceContext context, UUID id)
			throws Exception {
		Role role = em.get(context.getOwner(), Role.class);
		Entity entity = sm.getService("/users").getEntity(context.getRequest(),
				id);
		if (entity != null) {
			em.addUserToRole(entity.getUuid(), role.getRoleName());
		}
		return new ServiceResults(this, context, Type.COLLECTION,
				Results.fromRef(entity), null, null);
	}

	@Override
	public ServiceResults postItemByName(ServiceContext context, String name)
			throws Exception {
		Role role = em.get(context.getOwner(), Role.class);
		Entity entity = sm.getService("/users").getEntity(context.getRequest(),
				name);
		if (entity != null) {
			em.addUserToRole(entity.getUuid(), role.getRoleName());
		}
		return new ServiceResults(this, context, Type.COLLECTION,
				Results.fromRef(entity), null, null);
	}

	@Override
	public ServiceResults deleteItemById(ServiceContext context, UUID id)
			throws Exception {
		Role role = em.get(context.getOwner(), Role.class);
		ServiceResults results = getItemById(context, id);
		if (!results.isEmpty()) {
			em.removeUserFromRole(id, role.getRoleName());
		}
		return results;
	}

	@Override
	public ServiceResults deleteItemByName(ServiceContext context, String name)
			throws Exception {
		Role role = em.get(context.getOwner(), Role.class);
		ServiceResults results = getItemByName(context, name);
		if (!results.isEmpty()) {
			em.removeUserFromRole(results.getId(), role.getRoleName());
		}
		return results;
	}

}
