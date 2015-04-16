/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.android.sdk.response;

import static org.apache.usergrid.android.sdk.utils.JsonUtils.toJsonString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.usergrid.android.sdk.UGClient;
import org.apache.usergrid.android.sdk.entities.Entity;
import org.apache.usergrid.android.sdk.entities.Message;
import org.apache.usergrid.android.sdk.entities.User;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion;

public class ApiResponse {

	private String accessToken;

	private String error;
	private String errorDescription;
	private String errorUri;
	private String exception;

	private String path;
	private String uri;
	private String status;
	private long timestamp;
	private UUID application;
	private List<Entity> entities;
	private UUID next;
	private String cursor;
	private String action;
	private List<Object> list;
	private Object data;
	private Map<String, UUID> applications;
	private Map<String, JsonNode> metadata;
	private Map<String, List<String>> params;
	private List<AggregateCounterSet> counters;
	private ClientCredentialsInfo credentials;

	private List<Message> messages;
	private List<QueueInfo> queues;
	private UUID last;
	private UUID queue;
	private UUID consumer;

	private User user;
	private String rawResponse;

	private final Map<String, JsonNode> properties = new HashMap<String, JsonNode>();

	/**
	 * @y.exclude
	 */
	public ApiResponse() {
	}

	/**
    * Returns the 'properties' property of the request.
    *
    * @return a Map object of the properties
    */
	@JsonAnyGetter
	public Map<String, JsonNode> getProperties() {
		return properties;
	}

	/**
    * @y.exclude
    */
	@JsonAnySetter
	public void setProperty(String key, JsonNode value) {
		properties.put(key, value);
	}

	/**
    * Returns the OAuth token that was sent with the request
    *
    * @return the OAuth token
    */
	@JsonProperty("access_token")
	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getAccessToken() {
		return accessToken;
	}

	/**
    * @y.exclude
    */
	@JsonProperty("access_token")
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	/**
    * Returns the 'error' property of the response.
    *
    * @return the error
    */
	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getError() {
		return error;
	}

	/**
    * Sets the 'error' property of the response.
    *
    * @param  error  the error
    */
	public void setError(String error) {
		this.error = error;
	}

	/**
    * Returns the 'error_description' property of the response.
    *
    * @return the error description
    */
	@JsonSerialize(include = Inclusion.NON_NULL)
	@JsonProperty("error_description")
	public String getErrorDescription() {
		return errorDescription;
	}

	/**
    * Sets the 'error_description' property of the response.
    *
    * @param  errorDescription  the error description
    */
	@JsonProperty("error_description")
	public void setErrorDescription(String errorDescription) {
		this.errorDescription = errorDescription;
	}

	/**
    * Returns the 'error_uri' property of the response.
    *
    * @return the error URI
    */
	@JsonSerialize(include = Inclusion.NON_NULL)
	@JsonProperty("error_uri")
	public String getErrorUri() {
		return errorUri;
	}

	/**
    * Sets the 'error_uri' property of the response.
    *
    * @param  errorUri  the error URI
    */
	@JsonProperty("error_uri")
	public void setErrorUri(String errorUri) {
		this.errorUri = errorUri;
	}

	/**
    * Returns the 'exception' property of the response.
    *
    * @return the exception
    */
	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getException() {
		return exception;
	}

	/**
    * Sets the 'exception' property of the response.
    *
    * @param  exception  the exception
    */
	public void setException(String exception) {
		this.exception = exception;
	}

	/**
    * Returns the path of the request, i.e. the portion of the
    * request URI after the application name.
    *
    * @return the request path
    */
	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getPath() {
		return path;
	}

	/**
    * @y.exclude
    */
	public void setPath(String path) {
		this.path = path;
	}

	/**
    * Returns the full URI of the request.
    *
    * @return the full request URI
    */
	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getUri() {
		return uri;
	}

	/**
    * @y.exclude
    */
	public void setUri(String uri) {
		this.uri = uri;
	}

	/**
    * Returns the status property from the response. Only
    * applies to certain organization and application-level requests.
    *
    * @return the status
    */
	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getStatus() {
		return status;
	}

	/**
    * @y.exclude
    */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
    * Returns the timestamp of the response
    *
    * @return  the UNIX timestamp
    */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * @y.exclude
	 */
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	/**
    * Returns the UUID of the application that was targeted
    * by the request.
    *
    * @return the application UUID
    */
	@JsonSerialize(include = Inclusion.NON_NULL)
	public UUID getApplication() {
		return application;
	}

	/**
	 * @y.exclude
	 */
	public void setApplication(UUID application) {
		this.application = application;
	}

	/**
    * Returns the entities from the response as a List
    * of Entity objects.
    *
    * @return the entities
    */
	@JsonSerialize(include = Inclusion.NON_NULL)
	public List<Entity> getEntities() {
		return entities;
	}

	/**
	 * @y.exclude
	 */
	public void setEntities(List<Entity> entities) {
		this.entities = entities;
	}

	/**
    * Returns a count of the number of entities in the response.
    *
    * @return the number of entities in the response
    */
	public int getEntityCount() {
		if (entities == null) {
			return 0;
		}
		return entities.size();
	}

	/**
	 * Returns the first entity in the result set, or null
	 * if there were no entities.
	 *
	 * @return  an Entity object
	 * @see  org.apache.usergrid.android.sdk.entities.Entity 
	 */	
	public Entity getFirstEntity() {
		if ((entities != null) && (entities.size() > 0)) {
			return entities.get(0);
		}
		return null;
	}

	/**
	 * Returns the first entity in the result set.
	 *
	 * @return  an Entity object
	 * @see  org.apache.usergrid.android.sdk.entities.Entity 
	 */
	public <T extends Entity> T getFirstEntity(Class<T> t) {
		return Entity.toType(getFirstEntity(), t);
	}

	/**
	 * Returns the last entity in the result set.
	 *
	 * @return  an Entity object
	 * @see  org.apache.usergrid.android.sdk.entities.Entity 
	 */
	public Entity getLastEntity() {
		if ((entities != null) && (entities.size() > 0)) {
			return entities.get(entities.size() - 1);
		}
		return null;
	}

	/**
	 * Returns the last entity in the result set.
	 *
	 * @return  an Entity object
	 * @see  org.apache.usergrid.android.sdk.entities.Entity 
	 */
	public <T extends Entity> T getLastEntity(Class<T> t) {
		return Entity.toType(getLastEntity(), t);
	}

	/**
	 * Returns the a List of all entitie from the response.
	 *
	 * @return  a List object
	 * @see  org.apache.usergrid.android.sdk.entities.Entity 
	 */
	public <T extends Entity> List<T> getEntities(Class<T> t) {
		return Entity.toType(entities, t);
	}

	/**
	 * Returns the 'next' property of the response.
	 *
	 * @return  the 'next' property
	 */
	@JsonSerialize(include = Inclusion.NON_NULL)
	public UUID getNext() {
		return next;
	}

	/**
	 * @y.exclude
	 */
	public void setNext(UUID next) {
		this.next = next;
	}

	/**
    * Returns the cursor for retrieving the next page of results
    *
    * @return the cursor
    */
	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getCursor() {
		return cursor;
	}

	/**
	 * @y.exclude
	 */
	public void setCursor(String cursor) {
		this.cursor = cursor;
	}

	/**
	 * Returns the 'action' property from the response.
	 *
	 * @return  the 'action' property	 
	 */
	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getAction() {
		return action;
	}

	/**
	 * @y.exclude
	 */
	public void setAction(String action) {
		this.action = action;
	}

	/**
	 * Returns the 'list' property from the response.
	 *
	 * @return  the 'list' property	 
	 */
	@JsonSerialize(include = Inclusion.NON_NULL)
	public List<Object> getList() {
		return list;
	}

	/**
	 * @y.exclude
	 */
	public void setList(List<Object> list) {
		this.list = list;
	}

	/**
	 * Returns the 'data' property of the response from a
	 * request to create, retrieve or update an admin user.
	 *
	 * @return the 'data' property of the user entity
	 */
	@JsonSerialize(include = Inclusion.NON_NULL)
	public Object getData() {
		return data;
	}

	/**
	 * @y.exclude
	 */
	public void setData(Object data) {
		this.data = data;
	}

	/**
	 * For requests to get all applications in an organization, returns 
	 * the applications and their UUIDs as a Map object.
	 *
	 * @return the applications in the organization
	 */	
	@JsonSerialize(include = Inclusion.NON_NULL)
	public Map<String, UUID> getApplications() {
		return applications;
	}

	/**
	 * @y.exclude
	 */
	public void setApplications(Map<String, UUID> applications) {
		this.applications = applications;
	}

	/**
	 * Returns the 'metadata' property of the response as a Map object.
	 *
	 * @return the 'metadata' property
	 */	
	@JsonSerialize(include = Inclusion.NON_NULL)
	public Map<String, JsonNode> getMetadata() {
		return metadata;
	}

	/**
	 * @y.exclude
	 */
	public void setMetadata(Map<String, JsonNode> metadata) {
		this.metadata = metadata;
	}

	/**
	 * Returns the URL parameters that were sent with the request.
	 *
	 * @return the URL parameters of the request
	 */
	@JsonSerialize(include = Inclusion.NON_NULL)
	public Map<String, List<String>> getParams() {
		return params;
	}

	/**
	 * @y.exclude
	 */
	public void setParams(Map<String, List<String>> params) {
		this.params = params;
	}

	/**
	 * Returns the counters from the response.
	 *
	 * @return a List of the counters
	 */
	@JsonSerialize(include = Inclusion.NON_NULL)
	public List<AggregateCounterSet> getCounters() {
		return counters;
	}

	/**
	 * @y.exclude
	 */
	public void setCounters(List<AggregateCounterSet> counters) {
		this.counters = counters;
	}

	/**
	 * Returns the client id and client secret from the response. This is
	 * used only for requests that generate or retrieve credentials.
	 *
	 * @return the client id and client secret
	 */
	@JsonSerialize(include = Inclusion.NON_NULL)
	public ClientCredentialsInfo getCredentials() {
		return credentials;
	}

	/**
	 * @y.exclude
	 */
	public void setCredentials(ClientCredentialsInfo credentials) {
		this.credentials = credentials;
	}


	/**
	 * For requests to retrieve the admin users in an organization, returns 
	 * the 'user' property from the response.
	 *
	 * @return  a User object
	 */
	@JsonSerialize(include = Inclusion.NON_NULL)
	public User getUser() {
		return user;
	}

	/**
	 * @y.exclude
	 */
	public void setUser(User user) {
		this.user = user;
	}

	/**
	 * Returns the ApiResponse as a String
	 *
	 * @return  the ApiResponse in String format
	 */
	@Override
	public String toString() {
		return toJsonString(this);
	}

	/**
	 * For messaging queue requests, returns the 'messages' property.
	 *
	 * @return  the 'messages' property
	 */
	@JsonSerialize(include = Inclusion.NON_NULL)
	public List<Message> getMessages() {
		return messages;
	}

	/**
	 * @y.exclude
	 */
	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}

	/**
	 * For messaging queue requests, returns the number of messages
	 * in the response.
	 *
	 * @return  the number of messages in the 'messages' property
	 */
	@JsonIgnore
	public int getMessageCount() {
		if (messages == null) {
			return 0;
		}
		return messages.size();
	}

	/**
	 * For messaging queue requests, returns the first message
	 * in the response.
	 *
	 * @return  the first message in the 'messages' property
	 */
	@JsonIgnore
	public Message getFirstMessage() {
		if ((messages != null) && (messages.size() > 0)) {
			return messages.get(0);
		}
		return null;
	}

	/**
	 * For messaging queue requests, returns the last message
	 * in the response.
	 *
	 * @return  the last message in the 'messages' property
	 */
	@JsonIgnore
	public Entity getLastMessage() {
		if ((messages != null) && (messages.size() > 0)) {
			return messages.get(messages.size() - 1);
		}
		return null;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public UUID getLast() {
		return last;
	}

	/**
	 * @y.exclude
	 */
	public void setLast(UUID last) {
		this.last = last;
	}

	/**
	 * For messaging queue requests, returns the queues
	 * in the response.
	 *
	 * @return  the 'queues' property
	 */
	@JsonSerialize(include = Inclusion.NON_NULL)
	public List<QueueInfo> getQueues() {
		return queues;
	}

	/**
	 * @y.exclude
	 */
	public void setQueues(List<QueueInfo> queues) {
		this.queues = queues;
	}

	/**
	 * For messaging queue requests, returns the first queue
	 * in the response.
	 *
	 * @return  the first queue in the 'queues' property
	 */
	@JsonIgnore
	public QueueInfo getFirstQueue() {
		if ((queues != null) && (queues.size() > 0)) {
			return queues.get(0);
		}
		return null;
	}

	/**
	 * For messaging queue requests, returns the last queue
	 * in the response.
	 *
	 * @return  the last queue in the 'queues' property
	 */
	@JsonIgnore
	public QueueInfo getLastQueue() {
		if ((queues != null) && (queues.size() > 0)) {
			return queues.get(queues.size() - 1);
		}
		return null;
	}

	/**
	 * For messaging queue requests, returns the UUID of the
	 * last queue in the response.
	 *
	 * @return  the queue UUID
	 */
	@JsonIgnore
	public UUID getLastQueueId() {
		QueueInfo q = getLastQueue();
		if (q != null) {
			return q.getQueue();
		}
		return null;
	}

	/**
	 * For messaging queue requests, returns the UUID of the
	 * queue in the response.
	 *
	 * @return  the queue UUID
	 */
	@JsonSerialize(include = Inclusion.NON_NULL)
	public UUID getQueue() {
		return queue;
	}

	/**
	 * @y.exclude
	 */
	public void setQueue(UUID queue) {
		this.queue = queue;
	}

	/**
	 * Returns the 'consumer' property from message queue requests.
	 *
	 * @return the 'consumer' property
	 */	
	@JsonSerialize(include = Inclusion.NON_NULL)
	public UUID getConsumer() {
		return consumer;
	}

	/**
	 * @y.exclude
	 */
	public void setConsumer(UUID consumer) {
		this.consumer = consumer;
	}
	
	/**
	 * @y.exclude
	 */
	public void setRawResponse(String rawResponse) {
		this.rawResponse = rawResponse;
	}
	
	/**
	 * Returns the raw JSON response as a String.
	 *
	 * @return the JSON response
	 */	
	public String getRawResponse() {
		return rawResponse;
	}
	
	/**
	 * Sets the UGClient instance for all Entity objects in the response.
	 *
	 * @param  UGClient  an instance of UGClient
	 */
	public void setUGClient(UGClient client) {
		if( (entities != null) && !entities.isEmpty() ) {
			for ( Entity entity : entities ) {
				entity.setUGClient(client);
			}
		}
	}

}
