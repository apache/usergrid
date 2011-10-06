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

import static org.usergrid.utils.JsonUtils.normalizeJsonTree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.collections.iterators.EmptyIterator;
import org.apache.commons.collections.iterators.SingletonListIterator;
import org.usergrid.utils.JsonUtils;

public class ServicePayload {

	private final Map<String, Object> properties;
	private final List<Map<String, Object>> batch;
	private final List<UUID> list;

	public ServicePayload() {
		properties = new LinkedHashMap<String, Object>();
		batch = null;
		list = null;
	}

	private ServicePayload(Map<String, Object> properties,
			List<Map<String, Object>> batch, List<UUID> list) {
		this.properties = properties;
		this.batch = batch;
		this.list = list;
	}

	public static ServicePayload payload(Map<String, Object> properties) {
		return new ServicePayload(properties, null, null);
	}

	public static ServicePayload batchPayload(List<Map<String, Object>> batch) {
		return new ServicePayload(null, batch, null);
	}

	public static ServicePayload idListPayload(List<UUID> list) {
		return new ServicePayload(null, null, list);
	}

	@SuppressWarnings("unchecked")
	public static ServicePayload jsonPayload(Object json) {
		ServicePayload payload = null;
		json = normalizeJsonTree(json);
		if (json instanceof Map) {
			Map<String, Object> jsonMap = (Map<String, Object>) json;
			payload = payload(jsonMap);
		} else if (json instanceof List) {
			List<?> jsonList = (List<?>) json;
			if (jsonList.size() > 0) {
				if (jsonList.get(0) instanceof UUID) {
					payload = idListPayload((List<UUID>) json);
				} else if (jsonList.get(0) instanceof Map) {
					payload = ServicePayload
							.batchPayload((List<Map<String, Object>>) jsonList);
				}
			}
		}
		return payload;
	}

	public static ServicePayload stringPayload(String str) {
		return jsonPayload(JsonUtils.parse(str));
	}

	public boolean isBatch() {
		return batch != null;
	}

	public boolean isList() {
		return list != null;
	}

	public Map<String, Object> getProperties() {
		if (properties != null) {
			return properties;
		}
		if ((batch != null) && (batch.size() > 0)) {
			return batch.get(0);
		}
		return null;
	}

	public Object getProperty(String property) {
		Map<String, Object> p = getProperties();
		if (p == null) {
			return null;
		}
		return p.get(property);
	}

	public String getStringProperty(String property) {
		Object obj = getProperty(property);
		if (obj instanceof String) {
			return (String) obj;
		}
		return null;
	}

	public List<Map<String, Object>> getBatchProperties() {
		if (batch != null) {
			return batch;
		}
		if (properties != null) {
			List<Map<String, Object>> l = new ArrayList<Map<String, Object>>(1);
			l.add(properties);
			return l;
		}
		return null;
	}

	public List<UUID> getIdList() {
		return list;
	}

	@Override
	public String toString() {
		if (batch != null) {
			return JsonUtils.mapToJsonString(batch);
		} else if (list != null) {
			return JsonUtils.mapToJsonString(list);
		}
		return JsonUtils.mapToJsonString(properties);
	}

	@SuppressWarnings("unchecked")
	public Iterator<Map<String, Object>> payloadIterator() {
		if (isBatch()) {
			return batch.iterator();
		}
		if (properties != null) {
			return new SingletonListIterator(properties);
		}
		return EmptyIterator.INSTANCE;
	}

}
