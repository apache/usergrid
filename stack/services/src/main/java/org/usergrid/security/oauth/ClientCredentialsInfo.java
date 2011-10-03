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
package org.usergrid.security.oauth;

import java.util.UUID;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.usergrid.security.AuthPrincipalType;
import org.usergrid.utils.UUIDUtils;

public class ClientCredentialsInfo {

	private final String id;
	private final String secret;

	public ClientCredentialsInfo(String id, String secret) {
		this.id = id;
		this.secret = secret;
	}

	@JsonProperty("client_id")
	public String getId() {
		return id;
	}

	@JsonProperty("client_secret")
	public String getSecret() {
		return secret;
	}

	public static ClientCredentialsInfo forUuidAndSecret(
			AuthPrincipalType type, UUID uuid, String secret) {
		return new ClientCredentialsInfo(getClientIdForTypeAndUuid(type, uuid),
				secret);
	}

	public static String getClientIdForTypeAndUuid(AuthPrincipalType type,
			UUID uuid) {
		return type.getBase64Prefix() + UUIDUtils.toBase64(uuid);
	}

	@JsonIgnore
	public UUID getUUIDFromConsumerKey() {
		return getUUIDFromClientId(id);
	}

	public static UUID getUUIDFromClientId(String key) {
		if (key == null) {
			return null;
		}
		if (key.length() != 26) {
			return null;
		}
		return UUIDUtils.fromBase64(key.substring(4));
	}

	@JsonIgnore
	public AuthPrincipalType getTypeFromClientId() {
		return getTypeFromClientId(id);
	}

	public static AuthPrincipalType getTypeFromClientId(String key) {
		if (key == null) {
			return null;
		}
		if (key.length() != 26) {
			return null;
		}
		return AuthPrincipalType.getFromBase64String(key);
	}

}
