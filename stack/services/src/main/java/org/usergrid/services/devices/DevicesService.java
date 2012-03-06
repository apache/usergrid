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
package org.usergrid.services.devices;

import java.util.UUID;

import org.apache.commons.collections.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.entities.Device;
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
		logger.info("Registering device {}", id);
		if (deviceInCache(id)) {
			logger.info("Device {} in cache, skipping...", id);
			return new ServiceResults(this, context, Type.COLLECTION,
					Results.fromEntity(new Device(id)), null, null);
		} else {
			logger.info("Device {} not in cache, storing...", id);
			return super.putItemById(context, id);
		}
	}

	@Override
	public ServiceResults postItemById(ServiceContext context, UUID id)
			throws Exception {
		logger.info("Attempting to connect an entity to device {}", id);
		return super.postItemById(context, id);
	}

}
