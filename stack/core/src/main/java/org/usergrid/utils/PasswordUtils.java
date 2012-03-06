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
