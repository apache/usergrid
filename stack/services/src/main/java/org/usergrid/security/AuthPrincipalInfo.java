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
package org.usergrid.security;

import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
import static org.apache.commons.codec.digest.DigestUtils.sha;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;
import static org.usergrid.utils.ConversionUtils.bytes;
import static org.usergrid.utils.ConversionUtils.uuid;
import static org.usergrid.utils.StringUtils.stringOrSubstringAfterLast;
import static org.usergrid.utils.StringUtils.stringOrSubstringBeforeFirst;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.usergrid.security.tokens.exceptions.BadTokenException;

public class AuthPrincipalInfo {

	AuthPrincipalType type;
	UUID uuid;
	UUID applicationId;

	public AuthPrincipalInfo(AuthPrincipalType type, UUID uuid,
			UUID applicationId) {
		this.type = type;
		this.uuid = uuid;
		this.applicationId = applicationId;
	}

	public AuthPrincipalType getType() {
		return type;
	}

	public void setType(AuthPrincipalType type) {
		this.type = type;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public UUID getApplicationId() {
		return applicationId;
	}

	public void setApplicationId(UUID applicationId) {
		this.applicationId = applicationId;
	}

	public String constructAccessToken(String salt, String secret)
			throws Exception {

		long timestamp = System.currentTimeMillis();
		ByteBuffer bytes = ByteBuffer.allocate(28);
		bytes.putLong(timestamp);
		bytes.put(sha(timestamp + salt + secret + uuid));
		String digest = encodeBase64URLSafeString(bytes.array());
		return encodeBase64URLSafeString(bytes(uuid))
				+ ":"
				+ ((applicationId != null)
						&& !MANAGEMENT_APPLICATION_ID.equals(applicationId) ? encodeBase64URLSafeString(bytes(applicationId))
						+ ":"
						: "") + type.getBase64Prefix() + digest;
	}

	public static AuthPrincipalInfo getFromAccessToken(String token)
			throws BadTokenException {
		String uuidStr = stringOrSubstringBeforeFirst(token, ':');
		if (isEmpty(uuidStr)) {
			throw new BadTokenException("Unable to get uuid from token");
		}

		UUID uuid = null;
		try {
			uuid = uuid(decodeBase64(uuidStr));
		} catch (Exception e) {
			throw new BadTokenException("Unable to get entity from token");
		}

		UUID applicationId = MANAGEMENT_APPLICATION_ID;
		int first_colon = token.indexOf(':');
		if (first_colon >= 0) {
			int second_colon = token.indexOf(':', first_colon + 1);
			if (second_colon >= 0) {
				uuidStr = token.substring(first_colon + 1, second_colon);
				try {
					applicationId = uuid(decodeBase64(uuidStr));
				} catch (Exception e) {
					throw new BadTokenException(
							"Unable to get application from token");
				}
				if (applicationId == null) {
					applicationId = MANAGEMENT_APPLICATION_ID;
				}
			}
		}

		AuthPrincipalType type = AuthPrincipalType
				.getFromBase64String(stringOrSubstringAfterLast(token, ':'));

		if (type == null) {
			throw new BadTokenException("Unable to get type from token");
		}

		return new AuthPrincipalInfo(type, uuid, applicationId);

	}

	@Override
	public String toString() {
		return "AuthPrincipalInfo [type=" + type + ", uuid=" + uuid
				+ ", applicationId=" + applicationId + "]";
	}

}
