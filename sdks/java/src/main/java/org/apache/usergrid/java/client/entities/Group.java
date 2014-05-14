/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.java.client.entities;

import static com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion.NON_NULL;
import static org.apache.usergrid.java.client.utils.JsonUtils.getStringProperty;
import static org.apache.usergrid.java.client.utils.JsonUtils.setStringProperty;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class Group extends Entity {

	public final static String ENTITY_TYPE = "group";

	public final static String PROPERTY_PATH = "path";
	public final static String PROPERTY_TITLE = "title";

	public Group() {
		super();
		setType(ENTITY_TYPE);
	}

	public Group(Entity entity) {
		super();
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
		properties.add(PROPERTY_PATH);
		properties.add(PROPERTY_TITLE);
		return properties;
	}

	@JsonSerialize(include = NON_NULL)
	public String getPath() {
		return getStringProperty(properties, PROPERTY_PATH);
	}

	public void setPath(String path) {
		setStringProperty(properties, PROPERTY_PATH, path);
	}

	@JsonSerialize(include = NON_NULL)
	public String getTitle() {
		return getStringProperty(properties, PROPERTY_TITLE);
	}

	public void setTitle(String title) {
		setStringProperty(properties, PROPERTY_TITLE, title);
	}

}
