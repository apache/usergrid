/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Core.
 * 
 * Usergrid Core is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Usergrid Core is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Usergrid Core. If not, see <http://www.gnu.org/licenses/>.
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
