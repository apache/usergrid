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

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.EnumerationUtils;
import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.session.Session;

/**
 * Session that is only tied to an HttpServletRequest. This can be used for
 * applications that prefer to remain stateless.
 */
public class HttpServletRequestSession implements Session {
	private final HttpServletRequest request;
	private final String host;
	private final UUID uuid;
	private final Date start;

	public HttpServletRequestSession(HttpServletRequest request, String host) {
		this.request = request;
		this.host = host;
		uuid = UUID.randomUUID();
		start = new Date();
	}

	@Override
	public Serializable getId() {
		return uuid;
	}

	@Override
	public Date getStartTimestamp() {
		return start;
	}

	@Override
	public Date getLastAccessTime() {
		// the user only makes one request that involves this session
		return start;
	}

	@Override
	public long getTimeout() throws InvalidSessionException {
		return -1;
	}

	@Override
	public void setTimeout(long maxIdleTimeInMillis)
			throws InvalidSessionException {
		// ignore this - the session ends with the request and that's that...
	}

	@Override
	public String getHost() {
		return host;
	}

	@Override
	public void touch() throws InvalidSessionException {
		// do nothing - we don't timeout
	}

	@Override
	public void stop() throws InvalidSessionException {
		// do nothing - i don't have a use case for this and the structure to
		// support it, while not huge, adds
		// significant complexity
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public Collection<Object> getAttributeKeys() throws InvalidSessionException {
		return EnumerationUtils.toList(request.getAttributeNames());
	}

	@Override
	public Object getAttribute(Object key) throws InvalidSessionException {
		return request.getAttribute(stringify(key));
	}

	@Override
	public void setAttribute(Object key, Object value)
			throws InvalidSessionException {
		request.setAttribute(stringify(key), value);
	}

	@Override
	public Object removeAttribute(Object objectKey)
			throws InvalidSessionException {
		String key = stringify(objectKey);
		Object formerValue = request.getAttribute(key);
		request.removeAttribute(key);
		return formerValue;
	}

	private String stringify(Object key) {
		return key == null ? null : key.toString();
	}
}
