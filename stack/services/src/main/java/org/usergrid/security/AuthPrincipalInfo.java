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

import java.util.UUID;

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

	@Override
	public String toString() {
		return "AuthPrincipalInfo [type="
				+ type
				+ ", uuid="
				+ uuid
				+ (applicationId != null ? ", applicationId=" + applicationId
						: "") + "]";
	}

}
