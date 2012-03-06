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
