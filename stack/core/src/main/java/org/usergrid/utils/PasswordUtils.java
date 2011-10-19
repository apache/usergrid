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
package org.usergrid.utils;

import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;

import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.digest.DigestUtils;

public class PasswordUtils {

	public static boolean USE_BCRYPT = false;

	public static byte[] computeHash(String x) {
		java.security.MessageDigest d = null;
		try {
			d = java.security.MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		d.reset();
		d.update(x.getBytes());
		return d.digest();
	}

	public static String hashPassword(String password) throws Exception {
		if (USE_BCRYPT) {
			return BCrypt.hashpw(password, BCrypt.gensalt());
		} else {
			return encodeBase64URLSafeString(computeHash(password));
		}
	}

	public static String mongoPassword(String username, String password) {
		return DigestUtils.md5Hex(username + ":mongo:" + password);
	}

	public static boolean checkPassword(String password, String hash)
			throws Exception {
		if (USE_BCRYPT) {
			return BCrypt.checkpw(password, hash);
		} else {
			return hashPassword(password).equals(hash);
		}
	}

}
