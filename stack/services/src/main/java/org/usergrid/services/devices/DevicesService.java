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
package org.usergrid.services.devices;

import java.util.UUID;

import org.apache.commons.collections.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.Results;
import org.usergrid.services.AbstractCollectionService;
import org.usergrid.services.ServiceContext;
import org.usergrid.services.ServiceResults;
import org.usergrid.services.ServiceResults.Type;

public class DevicesService extends AbstractCollectionService {

	// registering devices can hit the DB hard, since badly-behaved apps can
	// call it very frequently. We need to maintain a simple LRU cache to
	// avoid this

	public static final int DEVICE_CACHE_COUNT = 10000;
	public static final long MAX_DEVICE_CACHE_AGE = 10 * 60 * 1000;

	private static final Logger logger = LoggerFactory
			.getLogger(DevicesService.class);

	private static LRUMap deviceCache = new LRUMap(DEVICE_CACHE_COUNT);

	public DevicesService() {
		super();
		logger.info("/devices");
	}

	static boolean deviceInCache(UUID deviceId) {
		synchronized (deviceCache) {
			Long timestamp = (Long) deviceCache.put(deviceId,
					System.currentTimeMillis());
			if (timestamp == null) {
				return false;
			}
			if ((timestamp - System.currentTimeMillis()) > MAX_DEVICE_CACHE_AGE) {
				deviceCache.remove(deviceId);
				return false;
			}
			return true;
		}
	}

	@Override
	public ServiceResults putItemById(ServiceContext context, UUID id)
			throws Exception {
		logger.info("Registering device " + id);
		if (deviceInCache(id)) {
			logger.info("Device " + id + " in cache, skipping...");
			return new ServiceResults(this, context, Type.COLLECTION, null,
					null, null);
		} else {
			logger.info("Device " + id + " not in cache, storing...");
			return super.putItemById(context, id);
		}
	}

	@Override
	public ServiceResults postItemById(ServiceContext context, UUID id)
			throws Exception {
		logger.info("Attempting to connect an entity to device " + id);
		Entity entity = em.get(id);
		if (entity == null) {
			return null;
		}
		entity = importEntity(context, entity);
		return new ServiceResults(this, context, Type.COLLECTION,
				Results.fromEntity(entity), null, null);
	}

}
