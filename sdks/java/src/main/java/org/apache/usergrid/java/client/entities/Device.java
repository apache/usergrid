package org.apache.usergrid.java.client.entities;

import static com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion.NON_NULL;
import static org.apache.usergrid.java.client.utils.JsonUtils.getStringProperty;
import static org.apache.usergrid.java.client.utils.JsonUtils.setStringProperty;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class Device extends Entity {

	public final static String ENTITY_TYPE = "device";

	public final static String PROPERTY_NAME = "name";

	public Device() {
		super();
		setType(ENTITY_TYPE);
	}

	public Device(Entity entity) {
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
		properties.add(PROPERTY_NAME);
		return properties;
	}

	@JsonSerialize(include = NON_NULL)
	public String getName() {
		return getStringProperty(properties, PROPERTY_NAME);
	}

	public void setName(String name) {
		setStringProperty(properties, PROPERTY_NAME, name);
	}

}
