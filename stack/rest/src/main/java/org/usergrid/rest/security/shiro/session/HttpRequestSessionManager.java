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
package org.usergrid.rest.security.shiro.session;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.SessionException;
import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.session.mgt.SessionKey;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.web.util.WebUtils;

/**
 * Intended to keep session request-scoped and therefore not persist them across
 * multiple requests - a user must login on each request. This necessarily means
 * that a mechanism like form-based authentication isn't viable, but the
 * intention is primarily for uses in stateless apis.
 */
public class HttpRequestSessionManager implements SessionManager {

	static final String REQUEST_ATTRIBUTE_KEY = "__SHIRO_REQUEST_SESSION";

	@Override
	public Session start(SessionContext context) throws AuthorizationException {
		if (!WebUtils.isHttp(context)) {
			String msg = "SessionContext must be an HTTP compatible implementation.";
			throw new IllegalArgumentException(msg);
		}

		HttpServletRequest request = WebUtils.getHttpRequest(context);

		String host = getHost(context);

		Session session = createSession(request, host);
		request.setAttribute(REQUEST_ATTRIBUTE_KEY, session);

		return session;
	}

	@Override
	public Session getSession(SessionKey key) throws SessionException {
		if (!WebUtils.isHttp(key)) {
			String msg = "SessionKey must be an HTTP compatible implementation.";
			throw new IllegalArgumentException(msg);
		}

		HttpServletRequest request = WebUtils.getHttpRequest(key);

		return (Session) request.getAttribute(REQUEST_ATTRIBUTE_KEY);
	}

	private String getHost(SessionContext context) {
		String host = context.getHost();
		if (host == null) {
			ServletRequest request = WebUtils.getRequest(context);
			if (request != null) {
				host = request.getRemoteHost();
			}
		}
		return host;

	}

	protected Session createSession(HttpServletRequest request, String host) {
		return new HttpServletRequestSession(request, host);
	}

}
