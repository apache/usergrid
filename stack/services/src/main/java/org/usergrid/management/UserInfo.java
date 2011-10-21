/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Stack.
 * 
 * Usergrid Stack is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * 
 * Usergrid Stack is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License along
 * with Usergrid Stack. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU AGPL version 3 section 7
 * 
 * Linking Usergrid Stack statically or dynamically with other modules is making
 * a combined work based on Usergrid Stack. Thus, the terms and conditions of the
 * GNU General Public License cover the whole combination.
 * 
 * In addition, as a special exception, the copyright holders of Usergrid Stack
 * give you permission to combine Usergrid Stack with free software programs or
 * libraries that are released under the GNU LGPL and with independent modules
 * that communicate with Usergrid Stack solely through:
 * 
 *   - Classes implementing the org.usergrid.services.Service interface
 *   - Apache Shiro Realms and Filters
 *   - Servlet Filters and JAX-RS/Jersey Filters
 * 
 * You may copy and distribute such a system following the terms of the GNU AGPL
 * for Usergrid Stack and the licenses of the other code concerned, provided that
 ******************************************************************************/
package org.usergrid.management;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.usergrid.persistence.Schema.PROPERTY_ACTIVATED;
import static org.usergrid.persistence.Schema.PROPERTY_DISABLED;
import static org.usergrid.persistence.Schema.PROPERTY_EMAIL;
import static org.usergrid.persistence.Schema.PROPERTY_NAME;
import static org.usergrid.persistence.Schema.PROPERTY_USERNAME;
import static org.usergrid.persistence.Schema.PROPERTY_UUID;
import static org.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;
import static org.usergrid.utils.ConversionUtils.getBoolean;
import static org.usergrid.utils.ConversionUtils.string;
import static org.usergrid.utils.ConversionUtils.uuid;

import java.util.Map;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserInfo {

	private final UUID applicationId;
	private final UUID id;
	private final String username;
	private final String name;
	private final String email;
	private final boolean activated;
	private final boolean disabled;

	public UserInfo(UUID applicationId, UUID id, String username, String name,
			String email, boolean activated, boolean disabled) {
		this.applicationId = applicationId;
		this.id = id;
		this.username = username;
		this.name = name;
		this.email = email;
		this.activated = activated;
		this.disabled = disabled;
	}

	public UserInfo(UUID applicationId, Map<String, Object> properties) {
		this.applicationId = applicationId;
		id = uuid(properties.get(PROPERTY_UUID));
		username = string(properties.get(PROPERTY_USERNAME));
		name = string(properties.get(PROPERTY_NAME));
		email = string(properties.get(PROPERTY_EMAIL));
		activated = getBoolean(properties.get(PROPERTY_ACTIVATED));
		disabled = getBoolean(properties.get(PROPERTY_DISABLED));
	}

	public UUID getApplicationId() {
		return applicationId;
	}

	public UUID getUuid() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getUsername() {
		return username;
	}

	public String getEmail() {
		return email;
	}

	@Override
	public String toString() {
		return id + "/" + name + "/" + email;
	}

	public String getDisplayEmailAddress() {
		if (isNotBlank(name)) {
			return name + " <" + email + ">";
		}
		return email;
	}

	public boolean isActivated() {
		return activated;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public boolean isAdminUser() {
		return MANAGEMENT_APPLICATION_ID.equals(applicationId);
	}

}
