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

package org.apache.usergrid.android.sdk.entities;

import static org.apache.usergrid.android.sdk.utils.JsonUtils.getBooleanProperty;
import static org.apache.usergrid.android.sdk.utils.JsonUtils.getUUIDProperty;
import static org.apache.usergrid.android.sdk.utils.JsonUtils.setBooleanProperty;
import static org.apache.usergrid.android.sdk.utils.JsonUtils.setLongProperty;
import static org.apache.usergrid.android.sdk.utils.JsonUtils.setStringProperty;
import static org.apache.usergrid.android.sdk.utils.JsonUtils.setUUIDProperty;
import static com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion.NON_NULL;

import java.util.List;
import java.util.UUID;

import org.apache.usergrid.android.sdk.UGClient;
import org.apache.usergrid.android.sdk.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion;

public class Message extends Entity {

	public static final String ENTITY_TYPE = "message";

	public static final String PROPERTY_CORRELATION_ID = "correlation_id";
	public static final String PROPERTY_DESTINATION = "destination";
	public static final String PROPERTY_REPLY_TO = "reply_to";
	public static final String PROPERTY_TIMESTAMP = "timestamp";
	public static final String PROPERTY_TYPE = "type";
	public static final String PROPERTY_CATEGORY = "category";
	public static final String PROPERTY_INDEXED = "indexed";
	public static final String PROPERTY_PERSISTENT = "persistent";

	public static boolean isSameType(String type) {
		return type.equals(ENTITY_TYPE);
	}

	public Message() {
		setType(ENTITY_TYPE);
	}
	
	public Message(UGClient client) {
		super(client);
		setType(ENTITY_TYPE);
	}

	public Message(Entity entity) {
		super(entity.getUGClient());
		properties = entity.properties;
		setType(ENTITY_TYPE);
	}

	@Override
	@JsonIgnore
	public String getNativeType() {
		return ENTITY_TYPE;
	}

	@Override
	@JsonIgnore
	public List<String> getPropertyNames() {
		List<String> properties = super.getPropertyNames();
		properties.add(PROPERTY_CORRELATION_ID);
		properties.add(PROPERTY_DESTINATION);
		properties.add(PROPERTY_REPLY_TO);
		properties.add(PROPERTY_TIMESTAMP);
		properties.add(PROPERTY_CATEGORY);
		properties.add(PROPERTY_INDEXED);
		properties.add(PROPERTY_PERSISTENT);
		return properties;
	}

	@JsonSerialize(include = NON_NULL)
	@JsonProperty(PROPERTY_CORRELATION_ID)
	public UUID getCorrelationId() {
		return getUUIDProperty(properties, PROPERTY_CORRELATION_ID);
	}

	@JsonProperty(PROPERTY_CORRELATION_ID)
	public void setCorrelationId(UUID uuid) {
		setUUIDProperty(properties, PROPERTY_CORRELATION_ID, uuid);
	}

	@JsonSerialize(include = NON_NULL)
	public String getDestination() {
		return getStringProperty(PROPERTY_DESTINATION);
	}

	public void setDestination(String destination) {
		setStringProperty(properties, PROPERTY_DESTINATION, destination);
	}

	@JsonSerialize(include = NON_NULL)
	@JsonProperty(PROPERTY_REPLY_TO)
	public String getReplyTo() {
		return getStringProperty(PROPERTY_DESTINATION);
	}

	@JsonProperty(PROPERTY_REPLY_TO)
	public void setReplyTo(String replyTo) {
		setStringProperty(properties, PROPERTY_DESTINATION, replyTo);
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public Long getTimestamp() {
		return JsonUtils.getLongProperty(properties, PROPERTY_TIMESTAMP);
	}

	public void setTimestamp(Long timestamp) {
		setLongProperty(properties, PROPERTY_TIMESTAMP, timestamp);
	}

	@JsonSerialize(include = NON_NULL)
	public String getCategory() {
		return getStringProperty(PROPERTY_CATEGORY);
	}

	public void setCategory(String category) {
		setStringProperty(properties, PROPERTY_CATEGORY, category);
	}

	@JsonSerialize(include = NON_NULL)
	public Boolean isIndexed() {
		return getBooleanProperty(properties, PROPERTY_INDEXED);
	}

	public void setIndexed(Boolean indexed) {
		setBooleanProperty(properties, PROPERTY_INDEXED, indexed);
	}

	@JsonSerialize(include = NON_NULL)
	public Boolean isPersistent() {
		return getBooleanProperty(properties, PROPERTY_INDEXED);
	}

	public void setPersistent(Boolean persistent) {
		setBooleanProperty(properties, PROPERTY_INDEXED, persistent);
	}

}
