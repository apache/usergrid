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
