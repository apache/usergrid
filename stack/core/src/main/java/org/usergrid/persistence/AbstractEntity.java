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
package org.usergrid.persistence;

import static org.usergrid.persistence.Schema.PROPERTY_NAME;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.usergrid.persistence.annotations.EntityProperty;

/**
 * The abstract superclass implementation of the Entity interface.
 * 
 * @author edanuff
 * 
 */
@XmlRootElement
public abstract class AbstractEntity implements Entity {

	protected UUID uuid;

	protected Long created;

	protected Long modified;

	protected Map<String, Object> dynamic_properties = new TreeMap<String, Object>(
			String.CASE_INSENSITIVE_ORDER);

	protected Map<String, Set<Object>> dynamic_sets = new TreeMap<String, Set<Object>>(
			String.CASE_INSENSITIVE_ORDER);

	@Override
	@EntityProperty(required = true, mutable = false, basic = true)
	@JsonSerialize(include = Inclusion.NON_NULL)
	public UUID getUuid() {
		return uuid;
	}

	@Override
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	@Override
	@EntityProperty(required = true, mutable = false, basic = true)
	public String getType() {
		return Schema.getDefaultSchema().getEntityType(this.getClass());
	}

	@Override
	public void setType(String type) {
	}

	@Override
	@EntityProperty(indexed = true, required = true, mutable = false)
	@JsonSerialize(include = Inclusion.NON_NULL)
	public Long getCreated() {
		return created;
	}

	@Override
	public void setCreated(Long created) {
		if (created == null) {
			created = System.currentTimeMillis();
		}
		this.created = created;
	}

	@Override
	@EntityProperty(indexed = true, required = true, mutable = true)
	@JsonSerialize(include = Inclusion.NON_NULL)
	public Long getModified() {
		return modified;
	}

	@Override
	public void setModified(Long modified) {
		if (modified == null) {
			modified = System.currentTimeMillis();
		}
		this.modified = modified;
	}

	@Override
	@JsonIgnore
	public String getName() {
		return (String) getProperty(PROPERTY_NAME);
	}

	@Override
	@JsonIgnore
	public Map<String, Object> getProperties() {
		return Schema.getDefaultSchema().getEntityProperties(this);
	}

	@Override
	public final Object getProperty(String propertyName) {
		return Schema.getDefaultSchema().getEntityProperty(this, propertyName);
	}

	@Override
	public final void setProperty(String propertyName, Object propertyValue) {
		Schema.getDefaultSchema().setEntityProperty(this, propertyName,
				propertyValue);
	}

	@Override
	public void setProperties(Map<String, Object> properties) {
		dynamic_properties = new TreeMap<String, Object>(
				String.CASE_INSENSITIVE_ORDER);
		addProperties(properties);
	}

	@Override
	public void addProperties(Map<String, Object> properties) {
		if (properties == null) {
			return;
		}
		for (Entry<String, Object> entry : properties.entrySet()) {
			setProperty(entry.getKey(), entry.getValue());
		}
	}

	@Override
	@JsonSerialize(include = Inclusion.NON_NULL)
	public Object getMetadata(String key) {
		return getDataset("metadata", key);
	}

	@Override
	public void setMetadata(String key, Object value) {
		setDataset("metadata", key, value);
	}

	@Override
	public void mergeMetadata(Map<String, Object> new_metadata) {
		mergeDataset("metadata", new_metadata);
	}

	@Override
	public void clearMetadata() {
		clearDataset("metadata");
	}

	public <T> T getDataset(String property, String key) {
		Object md = dynamic_properties.get(property);
		if (md == null) {
			return null;
		}
		if (!(md instanceof Map<?, ?>)) {
			return null;
		}
		@SuppressWarnings("unchecked")
		Map<String, T> metadata = (Map<String, T>) md;
		return metadata.get(key);
	}

	public <T> void setDataset(String property, String key, T value) {
		if (key == null) {
			return;
		}
		Object md = dynamic_properties.get(property);
		if (!(md instanceof Map<?, ?>)) {
			md = new HashMap<String, T>();
			dynamic_properties.put(property, md);
		}
		@SuppressWarnings("unchecked")
		Map<String, T> metadata = (Map<String, T>) md;
		metadata.put(key, value);
	}

	public <T> void mergeDataset(String property, Map<String, T> new_metadata) {
		Object md = dynamic_properties.get(property);
		if (!(md instanceof Map<?, ?>)) {
			md = new HashMap<String, T>();
			dynamic_properties.put(property, md);
		}
		@SuppressWarnings("unchecked")
		Map<String, T> metadata = (Map<String, T>) md;
		metadata.putAll(new_metadata);
	}

	public void clearDataset(String property) {
		dynamic_properties.remove(property);
	}

	@Override
	public List<Entity> getCollections(String key) {
		return getDataset("collections", key);
	}

	@Override
	public void setCollections(String key, List<Entity> results) {
		setDataset("collections", key, results);
	}

	@Override
	public List<Entity> getConnections(String key) {
		return getDataset("connections", key);
	}

	@Override
	public void setConnections(String key, List<Entity> results) {
		setDataset("connections", key, results);
	}

	@Override
	public String toString() {
		return "Entity(" + getProperties() + ")";
	}

	@Override
	@JsonAnySetter
	public void setDynamicProperty(String key, Object value) {
		dynamic_properties.put(key, value);
	}

	@Override
	@JsonAnyGetter
	public Map<String, Object> getDynamicProperties() {
		return dynamic_properties;
	}

	@Override
	public final int compareTo(Entity o) {
		if (o == null) {
			return 1;
		}
		try {
			long t1 = getUuid().timestamp();
			long t2 = o.getUuid().timestamp();
			return (t1 < t2) ? -1 : (t1 == t2) ? 0 : 1;
		} catch (UnsupportedOperationException e) {
		}
		return getUuid().compareTo(o.getUuid());
	}

	@Override
	public Entity toTypedEntity() {
		Entity entity = EntityFactory.newEntity(getUuid(), getType());
		entity.setProperties(getProperties());
		return entity;
	}

}
