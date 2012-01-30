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

import static org.usergrid.services.ServiceInfo.normalizeServicePattern;
import static org.usergrid.utils.ListUtils.dequeueCopy;
import static org.usergrid.utils.ListUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.Query;
import org.usergrid.services.exceptions.ServiceResourceNotFoundException;

public class ServiceContext {

	Service service;
	ServiceAction action;
	ServiceRequest request;
	ServiceResults previousResults;
	EntityRef owner;
	Query query;
	List<ServiceParameter> parameters;
	ServicePayload payload;
	String serviceMetadata;
	String collectionName;
	String serviceCommand;

	public ServiceContext() {
	}

	public ServiceContext(Service service, ServiceAction action,
			ServiceRequest request, ServiceResults previousResults,
			EntityRef owner, String collectionName, Query query,
			List<ServiceParameter> parameters, ServicePayload payload) {
		this.service = service;
		this.action = action;
		this.request = request;
		this.previousResults = previousResults;
		this.owner = owner;
		this.collectionName = collectionName;
		this.query = query;
		this.parameters = parameters;
		this.payload = payload;
	}

	public ServiceContext(Service service, ServiceAction action,
			ServiceRequest request, ServiceResults previousResults,
			EntityRef owner, String collectionName,
			List<ServiceParameter> parameters, ServicePayload payload) {
		this.service = service;
		this.action = action;
		this.request = request;
		this.previousResults = previousResults;
		this.owner = owner;
		this.collectionName = collectionName;
		this.parameters = parameters;
		this.payload = payload;
	}

	public ServiceAction getAction() {
		return action;
	}

	public void setAction(ServiceAction action) {
		this.action = action;
	}

	public ServiceContext withAction(ServiceAction action) {
		this.action = action;
		return this;
	}

	public ServiceRequest getRequest() {
		return request;
	}

	public String getPath() {
		return request.getPath();
	}

	public String getPath(String subPath) {
		return request.getPath() + "/" + subPath;
	}

	public String getPath(UUID entityId) {
		return request.getPath() + "/" + entityId.toString();
	}

	public String getPath(EntityRef entity) {
		return request.getPath() + "/" + entity.getUuid().toString();
	}

	public String getPath(UUID entityId, String subPath) {
		return request.getPath() + "/" + entityId.toString() + "/" + subPath;
	}

	public String getPath(EntityRef entity, String subPath) {
		return request.getPath() + "/" + entity.getUuid().toString() + "/"
				+ subPath;
	}

	public void setRequest(ServiceRequest request) {
		this.request = request;
	}

	public ServiceContext withRequest(ServiceRequest request) {
		this.request = request;
		return this;
	}

	public ServiceResults getPreviousResults() {
		return previousResults;
	}

	public void setPreviousResults(ServiceResults previousResults) {
		this.previousResults = previousResults;
	}

	public ServiceContext withPreviousResults(ServiceResults previousResults) {
		this.previousResults = previousResults;
		return this;
	}

	public EntityRef getOwner() {
		return owner;
	}

	public void setOwner(EntityRef owner) {
		this.owner = owner;
	}

	public ServiceContext withOwner(EntityRef owner) {
		this.owner = owner;
		return this;
	}

	public String getCollectionName() {
		return collectionName;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	public ServiceContext withCollectionName(String collectionName) {
		this.collectionName = collectionName;
		return this;
	}

	public Query getQuery() {
		return query;
	}

	public void setQuery(Query query) {
		this.query = query;
	}

	public ServiceContext withQuery(Query query) {
		this.query = query;
		return this;
	}

	public List<ServiceParameter> getParameters() {
		return parameters;
	}

	public void setParameters(List<ServiceParameter> parameters) {
		this.parameters = parameters;
	}

	public ServiceContext withParameters(List<ServiceParameter> parameters) {
		this.parameters = parameters;
		return this;
	}

	public ServicePayload getPayload() {
		return payload;
	}

	public Object getProperty(String property) {
		if (payload == null) {
			return null;
		}
		return payload.getProperty(property);
	}

	public Map<String, Object> getProperties() {
		if (payload == null) {
			return null;
		}
		return payload.getProperties();
	}

	public void setPayload(ServicePayload payload) {
		this.payload = payload;
	}

	public ServiceContext withPayload(ServicePayload payload) {
		this.payload = payload;
		return this;
	}

	public String getServiceMetadata() {
		return serviceMetadata;
	}

	public void setServiceMetadata(String serviceMetadata) {
		this.serviceMetadata = serviceMetadata;
	}

	public ServiceContext withServiceMetadata(String serviceMetadata) {
		this.serviceMetadata = serviceMetadata;
		return this;
	}

	public String getServiceCommand() {
		return serviceCommand;
	}

	public void setServiceCommand(String serviceCommand) {
		this.serviceCommand = serviceCommand;
	}

	public ServiceContext withServiceCommand(String serviceCommand) {
		this.serviceCommand = serviceCommand;
		return this;
	}

	public boolean isByUuid() {
		return ((query != null) && query.containsSingleUuidIdentifier());
	}

	public boolean isByName() {
		return ((query != null) && query.containsSingleNameOrEmailIdentifier());
	}

	public boolean isByQuery() {
		return ((query != null) && !isByUuid() && !isByName());
	}

	public UUID getUuid() {
		if (query != null) {
			return query.getSingleUuidIdentifier();
		}
		return null;
	}

	public String getName() {
		if (query != null) {
			return query.getSingleNameOrEmailIdentifier();
		}
		return null;
	}

	public boolean moreParameters() {
		return (parameters != null) && !parameters.isEmpty();
	}

	public ServiceParameter dequeueParameter() {
		return ServiceParameter.dequeueParameter(parameters);
	}

	public void queueParameter(ServiceParameter parameter) {
		ServiceParameter.queueParameter(parameters, parameter);
	}

	public ServiceParameter firstParameter() {
		if (parameters == null) {
			return null;
		}
		if (parameters.size() > 0) {
			return parameters.get(0);
		}
		return null;
	}

	public boolean firstParameterIsName() {
		return ServiceParameter.firstParameterIsName(parameters);
	}

	public boolean firstParameterIsQuery() {
		return ServiceParameter.firstParameterIsQuery(parameters);
	}

	public boolean firstParameterIsId() {
		return ServiceParameter.firstParameterIsId(parameters);
	}

	public int parameterCount() {
		return ServiceParameter.parameterCount(parameters);
	}

	public String getChildPath() {
		return request.getChildPath();
	}

	public List<ServiceRequest> getNextServiceRequests(EntityRef ref) {

		if (isEmpty(parameters)) {
			return null;
		}

		if (ref == null) {
			return null;
		}

		List<EntityRef> refs = new ArrayList<EntityRef>();
		refs.add(ref);

		return getNextServiceRequests(refs);
	}

	public List<ServiceRequest> getNextServiceRequests(List<EntityRef> refs) {

		if (isEmpty(parameters)) {
			return null;
		}

		if (isEmpty(refs)) {
			return null;
		}

		String next_service = null;

		String name = null;
		if (ServiceParameter.firstParameterIsName(parameters)) {
			name = parameters.get(0).toString();
			next_service = normalizeServicePattern(service.getServiceType()
					+ "/*/" + name);
			parameters = dequeueCopy(parameters);
		} else if (ServiceParameter.moreParameters(parameters)) {
			throw new ServiceResourceNotFoundException(this);
		} else {
			return null;
		}

		List<ServiceRequest> requests = null;

		if (next_service != null) {
			requests = new ArrayList<ServiceRequest>();
			for (EntityRef ref : refs) {
				ServiceRequest req = new ServiceRequest(request, ref,
						request.getPath() + "/" + ref.getUuid() + "/" + name,
						name, next_service, parameters);
				requests.add(req);
			}
		}

		return requests;
	}

}
