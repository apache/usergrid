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
package org.usergrid.java.client.entities;

import static com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion.NON_NULL;
import static org.usergrid.java.client.utils.JsonUtils.getBooleanProperty;
import static org.usergrid.java.client.utils.JsonUtils.getStringProperty;
import static org.usergrid.java.client.utils.JsonUtils.setBooleanProperty;
import static org.usergrid.java.client.utils.JsonUtils.setStringProperty;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class User extends Entity {

	public final static String ENTITY_TYPE = "user";

	public final static String PROPERTY_USERNAME = "username";
	public final static String PROPERTY_EMAIL = "email";
	public final static String PROPERTY_NAME = "name";
	public final static String PROPERTY_FIRSTNAME = "firstname";
	public final static String PROPERTY_MIDDLENAME = "middlename";
	public final static String PROPERTY_LASTNAME = "lastname";
	public final static String PROPERTY_ACTIVATED = "activated";
	public final static String PROPERTY_PICTURE = "picture";
	public final static String PROPERTY_DISABLED = "disabled";

	public User() {
		super();
		setType(ENTITY_TYPE);
	}

	public User(Entity entity) {
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
		properties.add(PROPERTY_USERNAME);
		properties.add(PROPERTY_EMAIL);
		properties.add(PROPERTY_NAME);
		properties.add(PROPERTY_FIRSTNAME);
		properties.add(PROPERTY_MIDDLENAME);
		properties.add(PROPERTY_LASTNAME);
		properties.add(PROPERTY_ACTIVATED);
		properties.add(PROPERTY_PICTURE);
		properties.add(PROPERTY_DISABLED);
		return properties;
	}

	@JsonSerialize(include = NON_NULL)
	public String getUsername() {
		return getStringProperty(properties, PROPERTY_USERNAME);
	}

	public void setUsername(String username) {
		setStringProperty(properties, PROPERTY_USERNAME, username);
	}

	@JsonSerialize(include = NON_NULL)
	public String getName() {
		return getStringProperty(properties, PROPERTY_NAME);
	}

	public void setName(String name) {
		setStringProperty(properties, PROPERTY_NAME, name);
	}

	@JsonSerialize(include = NON_NULL)
	public String getEmail() {
		return getStringProperty(properties, PROPERTY_EMAIL);
	}

	public void setEmail(String email) {
		setStringProperty(properties, PROPERTY_EMAIL, email);
	}

	@JsonSerialize(include = NON_NULL)
	public Boolean isActivated() {
		return getBooleanProperty(properties, PROPERTY_ACTIVATED);
	}

	public void setActivated(Boolean activated) {
		setBooleanProperty(properties, PROPERTY_ACTIVATED, activated);
	}

	@JsonSerialize(include = NON_NULL)
	public Boolean isDisabled() {
		return getBooleanProperty(properties, PROPERTY_DISABLED);
	}

	public void setDisabled(Boolean disabled) {
		setBooleanProperty(properties, PROPERTY_DISABLED, disabled);
	}

	@JsonSerialize(include = NON_NULL)
	public String getFirstname() {
		return getStringProperty(properties, PROPERTY_FIRSTNAME);
	}

	public void setFirstname(String firstname) {
		setStringProperty(properties, PROPERTY_FIRSTNAME, firstname);
	}

	@JsonSerialize(include = NON_NULL)
	public String getMiddlename() {
		return getStringProperty(properties, PROPERTY_MIDDLENAME);
	}

	public void setMiddlename(String middlename) {
		setStringProperty(properties, PROPERTY_MIDDLENAME, middlename);
	}

	@JsonSerialize(include = NON_NULL)
	public String getLastname() {
		return getStringProperty(properties, PROPERTY_LASTNAME);
	}

	public void setLastname(String lastname) {
		setStringProperty(properties, PROPERTY_LASTNAME, lastname);
	}

	@JsonSerialize(include = NON_NULL)
	public String getPicture() {
		return getStringProperty(properties, PROPERTY_PICTURE);
	}

	public void setPicture(String picture) {
		setStringProperty(properties, PROPERTY_PICTURE, picture);
	}

}
