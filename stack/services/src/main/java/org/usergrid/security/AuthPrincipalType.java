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
package org.usergrid.security;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.usergrid.utils.CodecUtils.base64;
import static org.usergrid.utils.StringUtils.stringOrSubstringAfterLast;
import static org.usergrid.utils.StringUtils.stringOrSubstringBeforeFirst;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.usergrid.persistence.entities.Group;
import org.usergrid.persistence.entities.User;

public enum AuthPrincipalType {
	ORGANIZATION("ou", Group.ENTITY_TYPE), ADMIN_USER("ad", User.ENTITY_TYPE), APPLICATION(
			"ap", "application_info"), APPLICATION_USER("au", User.ENTITY_TYPE);

	public static final int PREFIX_LENGTH = 3;
	public static final int BASE64_PREFIX_LENGTH = 4;

	private final String prefix;
	private final String base64Prefix;
	private final String entityType;

	private static Map<String, AuthPrincipalType> prefixes;
	private static Map<String, AuthPrincipalType> base64Prefixes;

	private synchronized static void register(AuthPrincipalType type) {
		if (prefixes == null) {
			prefixes = new ConcurrentHashMap<String, AuthPrincipalType>();
		}
		if (base64Prefixes == null) {
			base64Prefixes = new ConcurrentHashMap<String, AuthPrincipalType>();
		}
		prefixes.put(type.getPrefix(), type);
		base64Prefixes.put(type.getBase64Prefix(), type);
	}

	AuthPrincipalType(String prefix, String entityType) {
		this.prefix = prefix;
		base64Prefix = base64(prefix + ":");
		this.entityType = entityType;
		register(this);
	}

	public String getPrefix() {
		return prefix;
	}

	public String getBase64Prefix() {
		return base64Prefix;
	}

	public String getEntityType() {
		return entityType;
	}

	public boolean prefixesBase64String(String key) {
		if (key == null) {
			return false;
		}
		return key.startsWith(base64Prefix);
	}

	public static AuthPrincipalType getFromBase64String(String key) {
		if (key == null) {
			return null;
		}
		if (key.length() >= 4) {
			return base64Prefixes.get(key.substring(0, 4));
		}
		return null;
	}

	public boolean prefixesString(String key) {
		if (key == null) {
			return false;
		}
		return key.startsWith(prefix + ":");
	}

	public static AuthPrincipalType getFromString(String key) {
		if (key == null) {
			return null;
		}
		if ((key.length() >= 3) && (key.charAt(2) == ':')) {
			return prefixes.get(key.substring(0, 2));
		}
		return null;
	}

	public static AuthPrincipalType getFromAccessToken(String token) {
		String uuidStr = stringOrSubstringBeforeFirst(token, ':');
		if (isEmpty(uuidStr)) {
			return null;
		}
		String password = stringOrSubstringAfterLast(token, ':');
		if (isEmpty(password)) {
			return null;
		}
		return getFromBase64String(password);
	}

}
