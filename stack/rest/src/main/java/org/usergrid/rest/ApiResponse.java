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
package org.usergrid.rest;

import static org.usergrid.utils.InflectionUtils.pluralize;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.usergrid.persistence.AggregateCounterSet;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.entities.Application;
import org.usergrid.security.oauth.ClientCredentialsInfo;
import org.usergrid.services.ServiceRequest;
import org.usergrid.services.ServiceResults;
import org.usergrid.utils.InflectionUtils;

@JsonPropertyOrder({ "action", "application", "params", "path", "query", "uri",
		"status", "error", "applications", "entity", "entities", "list",
		"data", "next", "timestamp", "duration" })
@XmlRootElement
public class ApiResponse {

	UriInfo ui;
	ServiceRequest esp;

	String error;
	String errorDescription;
	String errorUri;
	String exception;
	String callback;

	String path;
	String uri;
	String status;
	long timestamp;
	UUID application;
	List<Entity> entities;
	UUID next;
	String cursor;
	String action;
	List<Object> list;
	Object data;
	Map<String, UUID> applications;
	Map<String, Object> metadata;
	Map<String, List<String>> params;
	List<AggregateCounterSet> counters;
	ClientCredentialsInfo credentials;

	protected Map<String, Object> properties = new TreeMap<String, Object>(
			String.CASE_INSENSITIVE_ORDER);

	public ApiResponse() {
		timestamp = System.currentTimeMillis();
	}

	public ApiResponse(UriInfo ui) {
		this.ui = ui;
		timestamp = System.currentTimeMillis();
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getCallback() {
		return callback;
	}

	public void setCallback(String callback) {
		this.callback = callback;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getError() {
		return error;
	}

	public void setError(String code) {
		error = code;
	}

	public static String exceptionToErrorCode(Throwable e) {
		if (e == null) {
			return "service_error";
		}
		String s = ClassUtils.getShortClassName(e.getClass());
		s = StringUtils.removeEnd(s, "Exception");
		s = InflectionUtils.underscore(s).toLowerCase();
		return s;
	}

	public ApiResponse withError(String code) {
		return withError(code, null, null);
	}

	public void setError(Throwable e) {
		setError(null, null, e);
	}

	public ApiResponse withError(Throwable e) {
		return withError(null, null, e);
	}

	public void setError(String description, Throwable e) {
		setError(null, description, e);
	}

	public ApiResponse withError(String description, Throwable e) {
		return withError(null, description, e);
	}

	public void setError(String code, String description, Throwable e) {
		if (code == null) {
			code = exceptionToErrorCode(e);
		}
		error = code;
		errorDescription = description;
		if (e != null) {
			if (description == null) {
				errorDescription = e.getMessage();
			}
			exception = e.getClass().getName();
		}
	}

	public ApiResponse withError(String code, String description, Throwable e) {
		setError(code, description, e);
		return this;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	@JsonProperty("error_description")
	public String getErrorDescription() {
		return errorDescription;
	}

	@JsonProperty("error_description")
	public void setErrorDescription(String errorDescription) {
		this.errorDescription = errorDescription;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	@JsonProperty("error_uri")
	public String getErrorUri() {
		return errorUri;
	}

	@JsonProperty("error_uri")
	public void setErrorUri(String errorUri) {
		this.errorUri = errorUri;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getException() {
		return exception;
	}

	public void setException(String exception) {
		this.exception = exception;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		if (path == null) {
			this.path = null;
			uri = null;
		}
		this.path = path;
		uri = ui.getBaseUri() + application.toString()
				+ (path != null ? path : "");
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getUri() {
		return uri;
	}

	public void setServiceRequest(ServiceRequest p) {
		esp = p;
		if (p != null) {
			path = p.getPath();
			uri = ui.getBaseUri() + application.toString()
					+ (path != null ? path : "");
		}
	}

	public ApiResponse withServiceRequest(ServiceRequest p) {
		setServiceRequest(p);
		return this;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getStatus() {
		return status;
	}

	public void setSuccess() {
		status = "ok";
	}

	public ApiResponse withSuccess() {
		status = "ok";
		return this;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public long getDuration() {
		return System.currentTimeMillis() - timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public ApiResponse withTimestamp(long timestamp) {
		this.timestamp = timestamp;
		return this;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public long getTimestamp() {
		return timestamp;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public ApiResponse withAction(String action) {
		this.action = action;
		return this;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public UUID getApplication() {
		return application;
	}

	public void setApplication(UUID applicationId) {
		if (applicationId != null) {
			application = applicationId;
		} else {
			application = new UUID(0, 0);
		}
		if (esp != null) {
			uri = ui.getBaseUri() + application.toString() + esp.toString();
		}
	}

	public ApiResponse withApplication(UUID applicationId) {
		setApplication(applicationId);
		return this;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	@XmlAnyElement
	public List<Entity> getEntities() {
		// prepareEntities();
		return entities;
	}

	public void setEntities(List<Entity> entities) {
		if (entities != null) {
			this.entities = entities;
		} else {
			this.entities = new ArrayList<Entity>();
		}
	}

	public ApiResponse withEntities(List<Entity> entities) {
		setEntities(entities);
		return this;
	}

	public void setResults(ServiceResults results) {
		if (results != null) {
			setPath(results.getPath());
			entities = results.getEntities();
			next = results.getNextResult();
			cursor = results.getCursor();
			counters = results.getCounters();
		} else {
			entities = new ArrayList<Entity>();
		}
	}

	public ApiResponse withResults(ServiceResults results) {
		setResults(results);
		return this;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public UUID getNext() {
		return next;
	}

	public void setNext(UUID next) {
		this.next = next;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getCursor() {
		return cursor;
	}

	public void setCursor(String cursor) {
		this.cursor = cursor;
	}

	public ApiResponse withEntity(Entity entity) {
		entities = new ArrayList<Entity>();
		entities.add(entity);
		return this;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public List<Object> getList() {
		return list;
	}

	public void setList(List<Object> list) {
		if (list != null) {
			this.list = list;
		} else {
			this.list = new ArrayList<Object>();
		}
	}

	public ApiResponse withList(List<Object> list) {
		setList(list);
		return this;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		if (data != null) {
			this.data = data;
		} else {
			this.data = new LinkedHashMap<String, Object>();
		}
	}

	public ApiResponse withData(Object data) {
		setData(data);
		return this;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public List<AggregateCounterSet> getCounters() {
		return counters;
	}

	public void setCounters(List<AggregateCounterSet> counters) {
		this.counters = counters;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public Map<String, UUID> getApplications() {
		return applications;
	}

	public void setApplications(Map<String, UUID> applications) {
		this.applications = applications;
	}

	public ApiResponse withApplications(Map<String, UUID> applications) {
		this.applications = applications;
		return this;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public ClientCredentialsInfo getCredentials() {
		return credentials;
	}

	public void setCredentials(ClientCredentialsInfo credentials) {
		this.credentials = credentials;
	}

	public ApiResponse withCredentials(ClientCredentialsInfo credentials) {
		this.credentials = credentials;
		return this;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public Map<String, List<String>> getParams() {
		return params;
	}

	public void setParams(Map<String, List<String>> params) {
		Map<String, List<String>> q = new LinkedHashMap<String, List<String>>();
		for (String k : params.keySet()) {
			List<String> v = params.get(k);
			if (v != null) {
				q.put(k, new ArrayList<String>(v));
			}
		}
		this.params = q;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

	public String getEntityPath(String url_base, Entity entity) {
		String entity_uri = null;
		if (!Application.ENTITY_TYPE.equals(entity.getType())) {
			entity_uri = url_base + application.toString() + "/"
					+ pluralize(entity.getType()) + "/" + entity.getUuid();
		} else {
			entity_uri = url_base + application.toString();
		}
		return entity_uri;
	}

	public void prepareEntities() {
		if (uri != null) {
			String url_base = ui.getBaseUri().toString();
			if (entities != null) {
				for (Entity entity : entities) {
					String entity_uri = getEntityPath(url_base, entity);
					entity.setMetadata("uri", entity_uri);
					entity.setMetadata("path", path + "/" + entity.getUuid());
				}
			}
		}
	}

	@JsonAnyGetter
	public Map<String, Object> getProperties() {
		return properties;
	}

	@JsonAnySetter
	public void setProperty(String key, Object value) {
		properties.put(key, value);
	}

}
