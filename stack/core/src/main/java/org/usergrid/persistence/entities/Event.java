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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.usergrid.persistence.TypedEntity;
import org.usergrid.persistence.annotations.EntityDictionary;
import org.usergrid.persistence.annotations.EntityProperty;

/**
 * An event type posted by the application.
 */
@XmlRootElement
public class Event extends TypedEntity {

	public static final String ENTITY_TYPE = "event";

	@EntityProperty(required = true, indexed = true, mutable = false)
	long timestamp = System.currentTimeMillis();

	@EntityProperty(required = false, indexed = true, mutable = false)
	UUID user;

	@EntityProperty(required = false, indexed = true, mutable = false)
	UUID group;

	@EntityProperty(fulltextIndexed = false, required = false, mutable = false)
	String category;

	@EntityProperty(indexed = false, required = false, mutable = false)
	Map<String, Integer> counters;

	@EntityProperty(indexed = false, required = false, mutable = false)
	String message;

	@EntityDictionary(keyType = java.lang.String.class)
	protected Set<String> connections;

	public Event() {
		// id = UUIDUtils.newTimeUUID();
	}

	public Event(UUID id) {
		uuid = id;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		if (timestamp == 0) {
			timestamp = System.currentTimeMillis();
		}
		this.timestamp = timestamp;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public UUID getUser() {
		return user;
	}

	public void setUser(UUID user) {
		this.user = user;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public UUID getGroup() {
		return group;
	}

	public void setGroup(UUID group) {
		this.group = group;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public Map<String, Integer> getCounters() {
		return counters;
	}

	public void setCounters(Map<String, Integer> counters) {
		this.counters = counters;
	}

	public void addCounter(String name, int value) {
		if (counters == null) {
			counters = new HashMap<String, Integer>();
		}
		counters.put(name, value);
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public Set<String> getConnections() {
		return connections;
	}

	public void setConnections(Set<String> connections) {
		this.connections = connections;
	}

}
