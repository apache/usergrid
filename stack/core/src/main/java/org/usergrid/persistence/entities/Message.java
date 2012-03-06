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
package org.usergrid.persistence.entities;

import java.util.Set;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.usergrid.persistence.TypedEntity;
import org.usergrid.persistence.annotations.EntityDictionary;
import org.usergrid.persistence.annotations.EntityProperty;

/**
 * A generic Message type for message queue type operations. For status updates
 * and other social actions, use Activity instead.
 */
@XmlRootElement
public class Message extends TypedEntity {

	public static final String ENTITY_TYPE = "message";

	@EntityProperty(name = "correlation_id", indexed = false, mutable = false)
	protected String correlationId;

	@EntityProperty(indexed = false, mutable = false)
	protected String destination;

	@EntityProperty(name = "reply_to", indexed = false, mutable = false)
	protected String replyTo;

	@EntityProperty(fulltextIndexed = false, required = true, mutable = false)
	String category;

	@EntityProperty(indexed = false, mutable = false)
	protected Boolean indexed;

	@EntityProperty(indexed = false, mutable = false)
	protected Boolean persistent;

	@EntityProperty(indexed = false, mutable = false)
	protected Long timestamp;

	@EntityDictionary(keyType = java.lang.String.class)
	protected Set<String> connections;

	public Message() {
		// id = UUIDUtils.newTimeUUID();
	}

	public Message(UUID id) {
		this.uuid = id;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	@JsonProperty("correlation_id")
	public String getCorrelationId() {
		return correlationId;
	}

	@JsonProperty("correlation_id")
	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	@JsonProperty("reply_to")
	public String getReplyTo() {
		return replyTo;
	}

	@JsonProperty("reply_to")
	public void setReplyTo(String replyTo) {
		this.replyTo = replyTo;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public Boolean getIndexed() {
		return indexed;
	}

	public void setIndexed(Boolean indexed) {
		this.indexed = indexed;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public Boolean getPersistent() {
		return persistent;
	}

	public void setPersistent(Boolean persistent) {
		this.persistent = persistent;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public Set<String> getConnections() {
		return connections;
	}

	public void setConnections(Set<String> connections) {
		this.connections = connections;
	}

}
