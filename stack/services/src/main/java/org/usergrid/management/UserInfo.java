/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    private final Map<String,Object> properties;

	public UserInfo(UUID applicationId, UUID id, String username, String name,
			String email, boolean activated, boolean disabled,
            Map<String,Object> properties) {
		this.applicationId = applicationId;
		this.id = id;
		this.username = username;
		this.name = name;
		this.email = email;
		this.activated = activated;
		this.disabled = disabled;
        this.properties = properties;
	}

	public UserInfo(UUID applicationId, Map<String, Object> properties) {
		this.applicationId = applicationId;
		id = uuid(properties.remove(PROPERTY_UUID));
		username = string(properties.remove(PROPERTY_USERNAME));
		name = string(properties.remove(PROPERTY_NAME));
		email = string(properties.remove(PROPERTY_EMAIL));
		activated = getBoolean(properties.remove(PROPERTY_ACTIVATED));
		disabled = getBoolean(properties.remove(PROPERTY_DISABLED));
        this.properties = properties;
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

	public String getHTMLDisplayEmailAddress() {
		if (isNotBlank(name)) {
			return name + " &lt;<a href=\"mailto:" + email + "\">" + email
					+ "</a>&gt;";
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

    public Map<String,Object> getProperties() {
        return properties;
    }

}
