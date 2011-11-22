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

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.usergrid.utils.ListUtils.isEmpty;
import static org.usergrid.utils.ListUtils.last;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.Query;
import org.usergrid.services.ServiceParameter.QueryParameter;
import org.usergrid.services.ServiceResults.Type;

public class ServiceRequest {

	private static final Logger logger = LoggerFactory.getLogger(ServiceRequest.class);

	public static final int MAX_INVOCATIONS = 10;

	public static long count = 0;

	private final long id = count++;

	private final ServiceManager services;

	private final ServiceAction action;
	private final ServiceRequest parent;
	private final EntityRef owner;
	private final String serviceName;
	private final String path;
	private final List<ServiceParameter> parameters;
	private final String childPath;
	private final boolean returnsTree;
	private final ServicePayload payload;

	// return results_set, result_entity, new_service, param_list, properties

	public ServiceRequest(ServiceManager services, ServiceAction action,
			String serviceName, List<ServiceParameter> parameters,
			ServicePayload payload, boolean returnsTree) {
		this.services = services;
		this.action = action;
		parent = null;
		owner = services.getApplicationRef();
		childPath = null;
		this.serviceName = serviceName;
		path = "/" + serviceName;
		this.parameters = parameters;
		this.returnsTree = returnsTree;
		if (payload == null) {
			payload = new ServicePayload();
		}

		this.payload = payload;
	}

	public ServiceRequest(ServiceManager services, ServiceAction action,
			String serviceName, List<ServiceParameter> parameters,
			ServicePayload payload) {
		this(services, action, serviceName, parameters, payload, false);
	}

	public ServiceRequest(ServiceRequest parent, EntityRef owner, String path,
			String childPath, String serviceName,
			List<ServiceParameter> parameters) {
		services = parent.services;
		returnsTree = parent.returnsTree;
		action = parent.action;
		payload = parent.payload;
		this.parent = parent;
		this.owner = owner;
		this.serviceName = serviceName;
		if (parameters == null) {
			parameters = new ArrayList<ServiceParameter>();
		}
		this.parameters = parameters;
		this.path = path;
		this.childPath = childPath;
	}

	public ServiceRequest(ServiceManager services, ServiceAction action,
			ServiceRequest parent, EntityRef owner, String path,
			String childPath, String serviceName,
			List<ServiceParameter> parameters, ServicePayload payload,
			boolean returnsTree) {
		this.services = services;
		this.action = action;
		this.parent = parent;
		this.owner = owner;
		this.serviceName = serviceName;
		this.path = path;
		this.parameters = parameters;
		this.childPath = childPath;
		this.returnsTree = returnsTree;
		this.payload = payload;
	}

	public static ServiceRequest withPath(ServiceRequest r, String path) {
		return new ServiceRequest(r.services, r.action, r.parent, r.owner,
				path, r.childPath, r.serviceName, r.parameters, r.payload,
				r.returnsTree);
	}

	public static ServiceRequest withChildPath(ServiceRequest r,
			String childPath) {
		return new ServiceRequest(r.services, r.action, r.parent, r.owner,
				r.path, childPath, r.serviceName, r.parameters, r.payload,
				r.returnsTree);
	}

	public ServiceRequest withPath(String path) {
		return withPath(this, path);
	}

	public ServiceRequest withChildPath(String childPath) {
		return withChildPath(this, childPath);
	}

	public long getId() {
		return id;
	}

	public String getPath() {
		return path;
	}

	public ServiceAction getAction() {
		return action;
	}

	public ServicePayload getPayload() {
		return payload;
	}

	public ServiceManager getServices() {
		return services;
	}

	public ServiceRequest getParent() {
		return parent;
	}

	public String getServiceName() {
		return serviceName;
	}

	public EntityRef getPreviousOwner() {
		if (parent == null) {
			return null;
		}
		return parent.getOwner();
	}

	public ServiceResults execute() throws Exception {
		return execute(null);
	}

	public ServiceResults execute(ServiceResults previousResults)
			throws Exception {

		// initServiceName();

		ServiceResults results = null;
		Service s = services.getService(serviceName);
		if (s != null) {
			results = s.invoke(action, this, previousResults, payload);
			if ((results != null) && results.hasMoreRequests()) {

				results = invokeMultiple(results, payload);
			}
		}

		if (results == null) {
			results = new ServiceResults(null, this, previousResults, null,
					Type.GENERIC, null, null, null);
		}

		return results;
	}

	private ServiceResults invokeMultiple(ServiceResults previousResults,
			ServicePayload payload) throws Exception {

		List<ServiceRequest> requests = previousResults.getNextRequests();
		if (requests.size() > MAX_INVOCATIONS) {
			throw new IllegalArgumentException(
					"Maximum sub-collection requests exceeded, limit is "
							+ MAX_INVOCATIONS + ", " + requests.size()
							+ " attempted");
		}

		if (returnsTree) {

			for (ServiceRequest request : requests) {

				ServiceResults rs = request.execute(previousResults);
				if (rs != null) {
					previousResults.setChildResults(rs);
				}

			}

			return previousResults;
		} else {
			ServiceResults aggregate_results = null;

			for (ServiceRequest request : requests) {

				ServiceResults rs = request.execute(previousResults);
				if (rs != null) {
					if (aggregate_results == null) {
						aggregate_results = rs;
					} else {
						aggregate_results.merge(rs);
					}
				}

			}

			return aggregate_results;
		}
	}

	public List<ServiceParameter> getParameters() {
		return parameters;
	}

	public boolean hasParameters() {
		return !isEmpty(parameters);
	}

	public EntityRef getOwner() {
		return owner;
	}

	public Query getLastQuery() {
		if (!isEmpty(parameters)) {
			return last(parameters).getQuery();
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (serviceName != null) {
			sb.append("/");
			sb.append(serviceName);
		}
		for (int i = 0; i < parameters.size(); i++) {
			ServiceParameter p = parameters.get(i);
			if (p instanceof QueryParameter) {
				if (i == (parameters.size() - 1)) {
					sb.append('?');
				} else {
					sb.append(';');
				}
				boolean has_prev_param = false;
				String q = p.toString();
				if (isNotBlank(q)) {
					try {
						sb.append("ql=" + URLEncoder.encode(q, "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						logger.error("Unable to encode url", e);
					}
					has_prev_param = true;
				}
				int limit = p.getQuery().getLimit();
				if (limit != Query.DEFAULT_LIMIT) {
					if (has_prev_param) {
						sb.append('&');
					}
					sb.append("limit=" + limit);
					has_prev_param = true;
				}
				if (p.getQuery().getStartResult() != null) {
					if (has_prev_param) {
						sb.append('&');
					}
					sb.append("start=" + p.getQuery().getStartResult());
					has_prev_param = true;
				}
			} else {
				sb.append('/');
				sb.append(p.toString());
			}
		}
		return sb.toString();
	}

	public String getChildPath() {
		return childPath;
	}

	public boolean isReturnsTree() {
		return returnsTree;
	}

}
