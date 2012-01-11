package org.usergrid.persistence.entities;

import java.util.List;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.usergrid.persistence.TypedEntity;
import org.usergrid.persistence.annotations.EntityCollection;
import org.usergrid.persistence.annotations.EntityProperty;

/**
 * The Device entity class for representing devices in the service.
 */
@XmlRootElement
public class Device extends TypedEntity {

	public static final String ENTITY_TYPE = "device";

	@EntityProperty(indexed = true, fulltextIndexed = false, required = false, indexedInConnections = true, aliasProperty = true, unique = true, basic = true)
	protected String name;

	@EntityCollection(type = "user", propertiesIndexed = {}, linkedCollection = "devices", indexingDynamicProperties = false)
	protected List<UUID> users;

	public Device() {
		// id = UUIDUtils.newTimeUUID();
	}

	public Device(UUID id) {
		uuid = id;
	}

	@Override
	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public List<UUID> getUsers() {
		return users;
	}

	public void setUsers(List<UUID> users) {
		this.users = users;
	}

}
