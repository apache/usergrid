package org.usergrid.persistence.entities;

import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.usergrid.persistence.TypedEntity;
import org.usergrid.persistence.annotations.EntityProperty;

/**
 * The Device entity class for representing devices in the service.
 */
@XmlRootElement
public class Device extends TypedEntity {

	public static final String ENTITY_TYPE = "device";

	@EntityProperty(indexed = true, fulltextIndexed = false, required = false, indexedInConnections = true, aliasProperty = true, unique = true, basic = true)
	protected String name;

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

}
