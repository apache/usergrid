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
			if ("connections".equals(name) && (parameters.size() > 1)
					&& parameters.get(1).isName()) {
				parameters = dequeueCopy(parameters);
				name = parameters.get(0).toString();
			}
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
