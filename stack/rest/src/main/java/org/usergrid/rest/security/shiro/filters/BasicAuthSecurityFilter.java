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
package org.usergrid.rest.security.shiro.filters;

import java.security.Principal;
import java.util.Map;

import javax.ws.rs.core.SecurityContext;

import org.apache.shiro.codec.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sun.jersey.spi.container.ContainerRequest;

@Component
public class BasicAuthSecurityFilter extends SecurityFilter {

	private static final Logger logger = LoggerFactory
			.getLogger(BasicAuthSecurityFilter.class);

	public BasicAuthSecurityFilter() {
		logger.info("BasicAuthSecurityFilter is installed");
	}

	@Override
	public ContainerRequest filter(ContainerRequest request) {
		Map<String, String> auth_types = getAuthTypes(request);
		if ((auth_types == null) || !auth_types.containsKey(AUTH_BASIC_TYPE)) {
			return request;
		}

		String[] values = Base64
				.decodeToString(auth_types.get(AUTH_BASIC_TYPE)).split(":");
		if (values.length < 2) {
			return request;
		}
		String name = values[0].toLowerCase();
		String password = values[1];

		String sysadmin_login_name = properties
				.getProperty("usergrid.sysadmin.login.name");
		String sysadmin_login_password = properties
				.getProperty("usergrid.sysadmin.login.password");
		boolean sysadmin_login_allowed = Boolean.parseBoolean(properties
				.getProperty("usergrid.sysadmin.login.allowed"));
		if (name.equals(sysadmin_login_name)
				&& password.equals(sysadmin_login_password)
				&& sysadmin_login_allowed) {
			request.setSecurityContext(new SysAdminRoleAuthenticator());
			logger.info("System administrator access allowed");
			return request;
		}

		return request;
	}

	private static class SysAdminRoleAuthenticator implements SecurityContext {

		private final Principal principal;

		SysAdminRoleAuthenticator() {
			principal = new Principal() {
				@Override
				public String getName() {
					return "sysadmin";
				}
			};
		}

		@Override
		public Principal getUserPrincipal() {
			return principal;
		}

		@Override
		public boolean isUserInRole(String role) {
			return role.equals("sysadmin");
		}

		@Override
		public boolean isSecure() {
			return false;
		}

		@Override
		public String getAuthenticationScheme() {
			return SecurityContext.BASIC_AUTH;
		}
	}
}
