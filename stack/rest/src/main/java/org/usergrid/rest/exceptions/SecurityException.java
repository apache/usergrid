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
package org.usergrid.rest.exceptions;

import static org.usergrid.utils.JsonUtils.mapToJsonString;

import org.usergrid.rest.ApiResponse;

import com.sun.jersey.api.container.MappableContainerException;

/**
 * <p>
 * A runtime exception representing a failure to provide correct authentication
 * credentials. Will result in the browser presenting a password challenge if a
 * realm is provided.
 * </p>
 */
public class SecurityException extends RuntimeException {

	public static final String REALM = "Usergrid Authentication";

	private static final long serialVersionUID = 1L;

	private String realm = null;
	private String type = null;

	private SecurityException(String type, String message, String realm) {
		super(message);
		this.type = type;
		this.realm = realm;
	}

	public String getRealm() {
		return realm;
	}

	public String getType() {
		return type;
	}

	public String getJsonResponse() {
		ApiResponse response = new ApiResponse();
		response.setError(type, getMessage(), this);
		return mapToJsonString(response);
	}

	public static MappableContainerException mappableSecurityException(
			AuthErrorInfo errorInfo) {
		return mappableSecurityException(errorInfo.getType(),
				errorInfo.getMessage());
	}

	public static MappableContainerException mappableSecurityException(
			AuthErrorInfo errorInfo, String message) {
		return mappableSecurityException(errorInfo.getType(), message);
	}

	public static MappableContainerException mappableSecurityException(
			String type, String message) {
		return new MappableContainerException(new SecurityException(type,
				message, null));
	}

	public static MappableContainerException mappableSecurityException(
			AuthErrorInfo errorInfo, String message, String realm) {
		return mappableSecurityException(errorInfo.getType(), message, realm);
	}

	public static MappableContainerException mappableSecurityException(
			String type, String message, String realm) {
		return new MappableContainerException(new SecurityException(type,
				message, realm));
	}

}
