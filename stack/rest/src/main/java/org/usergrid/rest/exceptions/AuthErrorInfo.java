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

import org.usergrid.management.exceptions.BadAccessTokenException;
import org.usergrid.management.exceptions.DisabledAdminUserException;
import org.usergrid.management.exceptions.ExpiredTokenException;
import org.usergrid.management.exceptions.IncorrectPasswordException;
import org.usergrid.management.exceptions.InvalidAccessTokenException;
import org.usergrid.management.exceptions.UnactivatedAdminUserException;
import org.usergrid.management.exceptions.UnactivatedOrganizationException;

public enum AuthErrorInfo {

	OAUTH2_INVALID_REQUEST("invalid_request", "Unable to authenticate (OAuth)"), //
	OAUTH2_INVALID_CLIENT("invalid_client", "Unable to authenticate (OAuth)"), //
	OAUTH2_INVALID_GRANT("invalid_grant", "Unable to authenticate (OAuth)"), //
	OAUTH2_UNAUTHORIZED_CLIENT("unauthorized_client",
			"Unable to authenticate (OAuth)"), //
	OAUTH2_UNSUPPORTED_GRANT_TYPE("unsupported_grant_type",
			"Unable to authenticate (OAuth)"), //
	OAUTH2_INVALID_SCOPE("invalid_scope", "Unable to authenticate (OAuth"), //
	INVALID_AUTH_ERROR("auth_invalid", "Unable to authenticate"), //
	MISSING_CREDENTIALS_ERROR("auth_missing_credentials",
			"Unable to authenticate due to missing credentials"), //
	BAD_CREDENTIALS_SYNTAX_ERROR("auth_bad_credentials_syntax",
			"Unable to authenticate due to improperly constructed credentials"), //
	BLANK_USERNAME_OR_PASSWORD_ERROR("auth_blank_username_or_password",
			"Unable to authenticate due to username or password being empty"), //
	INVALID_USERNAME_OR_PASSWORD_ERROR("auth_invalid_username_or_password",
			"Unable to authenticate due to username or password being incorrect"), //
	UNVERIFIED_OAUTH_ERROR("auth_unverified_oath",
			"Unable to authenticate OAuth credentials"), //
	NO_DOMAIN_ERROR("auth_no_application",
			"Unable to authenticate due to application not found"), //
	NOT_DOMAIN_OWNER_ERROR("auth_not_application_owner", ""), //
	EXPIRED_ACCESS_TOKEN_ERROR("expired_token",
			"Unable to authenticate due to expired access token"), //
	BAD_ACCESS_TOKEN_ERROR("auth_bad_access_token",
			"Unable to authenticate due to corrupt access token"), //
	MISSING_OAUTH_KEY("oauth_missing_key",
			"Unable to authenticate due to missing consumer key"), //
	MISSING_OAUTH_SECRET("oauth_missing_secret",
			"Unable to authenticate due to missing secret key"), //
	INCORRECT_OAUTH_KEY("oauth_incorrect_key",
			"Unable to authenticate due to incorrect key"), //
	UNACTIVATED_ORGANIZATION("auth_unactivated_organization",
			"Unable to authenticate due to organization not activated"), //
	DISABLED_ORGANIZATION("auth_disabled_organization",
			"Unable to authenticate due to organization access being disabled"), //
	UNACTIVATED_ADMIN("auth_unactivated_admin",
			"Unable to authenticate due to admin user not activated"), //
	DISABLED_ADMIN("auth_disabled_admin",
			"Unable to authenticate due to admin user access disabled");

	private final String type;
	private final String message;

	AuthErrorInfo(String type, String message) {
		this.type = type;
		this.message = message;
	}

	public String getType() {
		return type;
	}

	public String getMessage() {
		return message;
	}

	public static AuthErrorInfo getForException(Throwable e) {
		if (e instanceof DisabledAdminUserException) {
			return DISABLED_ADMIN;
		} else if (e instanceof ExpiredTokenException) {
			return EXPIRED_ACCESS_TOKEN_ERROR;
		} else if (e instanceof IncorrectPasswordException) {
			return INVALID_USERNAME_OR_PASSWORD_ERROR;
		} else if (e instanceof InvalidAccessTokenException) {
			return BAD_ACCESS_TOKEN_ERROR;
		} else if (e instanceof UnactivatedOrganizationException) {
			return UNACTIVATED_ORGANIZATION;
		} else if (e instanceof UnactivatedAdminUserException) {
			return UNACTIVATED_ADMIN;
		} else if (e instanceof BadAccessTokenException) {
			return BAD_ACCESS_TOKEN_ERROR;
		}
		return null;
	}
}
