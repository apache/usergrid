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
package org.usergrid.services;

import java.util.UUID;

import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityRef;

public interface Service {

	public static final String GENERIC_ENTITY_TYPE = "entity";

	public String getServiceType();

	public Class<? extends Entity> getEntityClass();

	public String getEntityType();

	public boolean isRootService();

	public ServiceResults invoke(ServiceAction action, ServiceRequest request,
			ServiceResults previousResults, ServicePayload payload)
			throws Exception;

	public Entity getEntity(ServiceRequest request, UUID uuid) throws Exception;

	public Entity getEntity(ServiceRequest request, String name)
			throws Exception;

	public Entity importEntity(ServiceRequest request, Entity entity)
			throws Exception;

	public Entity writeEntity(ServiceRequest request, Entity entity)
			throws Exception;

	public Entity updateEntity(ServiceRequest request, EntityRef ref,
			ServicePayload payload) throws Exception;

}
