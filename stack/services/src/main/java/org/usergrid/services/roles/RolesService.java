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
package org.usergrid.services.roles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.SimpleRoleRef;
import org.usergrid.persistence.entities.Group;
import org.usergrid.services.AbstractCollectionService;
import org.usergrid.services.ServiceContext;
import org.usergrid.services.ServiceResults;

public class RolesService extends AbstractCollectionService {

	private static final Logger logger = LoggerFactory.getLogger(RolesService.class);

	public RolesService() {
		super();
		logger.info("/roles");

		declareEntityDictionary("permissions");

	}

	@Override
	public ServiceResults getItemByName(ServiceContext context, String name)
			throws Exception {
		if ((context.getOwner() != null)
				&& Group.ENTITY_TYPE.equals(context.getOwner().getType())) {
			return getItemById(context,
					SimpleRoleRef.getIdForGroupIdAndRoleName(context.getOwner()
							.getUuid(), name));
		}
		return super.getItemByName(context, name);
	}

}
