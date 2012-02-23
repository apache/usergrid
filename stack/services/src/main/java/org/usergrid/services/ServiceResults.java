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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.AggregateCounterSet;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;

public class ServiceResults extends Results {

	private static final Logger logger = LoggerFactory
			.getLogger(ServiceResults.class);

	public enum Type {
		GENERIC, COLLECTION, CONNECTION, COUNTERS
	}

	private final Service service;
	private final ServiceRequest request;
	private final Map<String, Object> serviceMetadata;

	private final List<ServiceRequest> nextRequests;

	private final String path;

	private final String childPath;

	private final Type resultsType;

	private final ServiceResults previousResults;

	public ServiceResults(Service service, ServiceRequest request,
			ServiceResults previousResults, String childPath, Type resultsType,
			Results r, Map<String, Object> serviceMetadata,
			List<ServiceRequest> nextRequests) {
		super(r);
		this.service = service;
		this.request = request;
		this.previousResults = previousResults;
		this.childPath = childPath;
		this.resultsType = resultsType;
		if (request != null) {
			path = request.getPath();
		} else {
			path = null;
		}
		this.serviceMetadata = serviceMetadata;
		this.nextRequests = nextRequests;
		logger.info("Child path: {}",childPath);
	}

	public ServiceResults(Service service, ServiceContext context,
			Type resultsType, Results r, Map<String, Object> serviceMetadata,
			List<ServiceRequest> nextRequests) {
		super(r);
		this.service = service;
		request = context.getRequest();
		previousResults = context.getPreviousResults();
		childPath = context.getRequest().getChildPath();
		this.resultsType = resultsType;
		if (request != null) {
			path = request.getPath();
		} else {
			path = null;
		}
		this.serviceMetadata = serviceMetadata;
		this.nextRequests = nextRequests;
		logger.info("Child path: {}",childPath);
	}

	public static ServiceResults genericServiceResults() {
		return new ServiceResults(null, null, null, null, Type.GENERIC, null,
				null, null);
	}

	public static ServiceResults genericServiceResults(Results r) {
		return new ServiceResults(null, null, null, null, Type.GENERIC, r,
				null, null);
	}

	public static ServiceResults simpleServiceResults(Type resultsType) {
		return new ServiceResults(null, null, null, null, resultsType, null,
				null, null);
	}

	public static ServiceResults simpleServiceResults(Type resultsType,
			Results r) {
		return new ServiceResults(null, null, null, null, resultsType, r, null,
				null);
	}

	public Service getService() {
		return service;
	}

	public ServiceRequest getRequest() {
		return request;
	}

	public ServiceResults getPreviousResults() {
		return previousResults;
	}

	public Map<String, Object> getServiceMetadata() {
		return serviceMetadata;
	}

	public boolean hasSelection() {
		if (request == null) {
			return false;
		}
		Query q = getQuery();
		if (q != null) {
			return q.hasSelectSubjects();
		}
		return false;
	}

	public List<Object> getSelectionResults() {
		Query q = getQuery();
		if (q == null) {
			return null;
		}
		return q.getSelectionResults(this);
	}

	public Object getSelectionResult() {
		Query q = getQuery();
		if (q == null) {
			return null;
		}
		return q.getSelectionResult(this);
	}

	public String getPath() {
		return path;
	}

	public List<ServiceRequest> getNextRequests() {
		return nextRequests;
	}

	public boolean hasMoreRequests() {
		return (nextRequests != null) && (nextRequests.size() > 0);
	}

	public String getChildPath() {
		return childPath;
	}

	public Type getResultsType() {
		return resultsType;
	}

	public void setChildResults(ServiceResults childResults) {
		setChildResults(childResults.getResultsType(), childResults
				.getRequest().getOwner().getUuid(),
				childResults.getChildPath(), childResults.getEntities());
	}

	public void setChildResults(Type rtype, UUID id, String name,
			List<Entity> results) {
		if ((results == null) || (results.size() == 0)) {
			return;
		}
		if (rtype == Type.GENERIC) {
			return;
		}
		List<Entity> entities = getEntities();
		if (entities != null) {
			for (Entity entity : entities) {
				if (entity.getUuid().equals(id)) {
					if (rtype == Type.COLLECTION) {
						entity.setCollections(name, results);
					} else if (rtype == Type.CONNECTION) {
						entity.setConnections(name, results);
					}
				}
			}
		}
	}

	@Override
	public ServiceResults withQuery(Query query) {
		return (ServiceResults) super.withQuery(query);
	}

	@Override
	public ServiceResults withIds(List<UUID> resultsIds) {
		return (ServiceResults) super.withIds(resultsIds);
	}

	@Override
	public ServiceResults withRefs(List<EntityRef> resultsRefs) {
		return (ServiceResults) super.withRefs(resultsRefs);
	}

	@Override
	public ServiceResults withRef(EntityRef ref) {
		return (ServiceResults) super.withRef(ref);
	}

	@Override
	public ServiceResults withEntity(Entity resultEntity) {
		return (ServiceResults) super.withEntity(resultEntity);
	}

	@Override
	public ServiceResults withEntities(List<? extends Entity> resultsEntities) {
		return (ServiceResults) super.withEntities(resultsEntities);
	}

	@Override
	public ServiceResults withDataName(String dataName) {
		return (ServiceResults) super.withDataName(dataName);
	}

	@Override
	public ServiceResults withCounters(List<AggregateCounterSet> counters) {
		return (ServiceResults) super.withCounters(counters);
	}

	@Override
	public ServiceResults withNextResult(UUID nextResult) {
		return (ServiceResults) super.withNextResult(nextResult);
	}

	@Override
	public ServiceResults withCursor(String cursor) {
		return (ServiceResults) super.withCursor(cursor);
	}

	@Override
	public ServiceResults withMetadata(UUID id, String name, Object value) {
		return (ServiceResults) super.withMetadata(id, name, value);
	}

	@Override
	public ServiceResults withMetadata(UUID id, Map<String, Object> data) {
		return (ServiceResults) super.withMetadata(id, data);
	}

	@Override
	public ServiceResults withMetadata(Map<UUID, Map<String, Object>> metadata) {
		return (ServiceResults) super.withMetadata(metadata);
	}

	@Override
	public ServiceResults withData(Object data) {
		return (ServiceResults) super.withData(data);
	}

}
