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
package org.usergrid.rest.security;

import org.apache.log4j.Logger;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

public class CrossOriginRequestFilter implements ContainerResponseFilter {

	public static final Logger logger = Logger
			.getLogger(CrossOriginRequestFilter.class);

	private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
	private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
	private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
	private static final String ACCESS_CONTROL_REQUEST_METHOD = "access-control-request-method";
	private static final String ACCESS_CONTROL_REQUEST_HEADERS = "access-control-request-headers";
	private static final String ORIGIN_HEADER = "origin";
	private static final String REFERER_HEADER = "referer";
	private static final String AUTHORIZATION_HEADER = "authorization";

	@Override
	public ContainerResponse filter(ContainerRequest request,
			ContainerResponse response) {

		// logger.info(JsonUtils.mapToFormattedJsonString(request
		// .getRequestHeaders()));

		if (request.getRequestHeaders().containsKey(
				ACCESS_CONTROL_REQUEST_METHOD)) {

			for (String value : request.getRequestHeaders().get(
					ACCESS_CONTROL_REQUEST_METHOD)) {
				response.getHttpHeaders().add(ACCESS_CONTROL_ALLOW_METHODS,
						value);
			}
		}

		if (request.getRequestHeaders().containsKey(
				ACCESS_CONTROL_REQUEST_HEADERS)) {
			for (String value : request.getRequestHeaders().get(
					ACCESS_CONTROL_REQUEST_HEADERS)) {
				response.getHttpHeaders().add(ACCESS_CONTROL_ALLOW_HEADERS,
						value);
			}
		}

		boolean origin_sent = false;
		boolean null_origin_received = false;
		if (request.getRequestHeaders().containsKey(ORIGIN_HEADER)) {
			for (String value : request.getRequestHeaders().get(ORIGIN_HEADER)) {
				if (value != null) {
					if ("null".equalsIgnoreCase(value)) {
						null_origin_received = true;
					} else {
						origin_sent = true;
						response.getHttpHeaders().add(
								ACCESS_CONTROL_ALLOW_ORIGIN, value);
					}
				}
			}
		}

		if (!origin_sent) {
			String origin = getOrigin(request);
			if (origin != null) {
				response.getHttpHeaders().add(ACCESS_CONTROL_ALLOW_CREDENTIALS,
						"true");
				response.getHttpHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN,
						origin);
			} else {
				if (!null_origin_received) {
					if (!request.getRequestHeaders().containsKey(
							AUTHORIZATION_HEADER)) {
						response.getHttpHeaders().add(
								ACCESS_CONTROL_ALLOW_ORIGIN, "*");
					}
				}
			}
		} else {
			response.getHttpHeaders().add(ACCESS_CONTROL_ALLOW_CREDENTIALS,
					"true");
		}

		// logger.info(JsonUtils.mapToFormattedJsonString(response
		// .getHttpHeaders()));

		return response;
	}

	public String getOrigin(ContainerRequest request) {
		String origin = request.getRequestHeaders().getFirst(ORIGIN_HEADER);
		String referer = request.getRequestHeaders().getFirst(REFERER_HEADER);
		if ((origin != null) && (!"null".equalsIgnoreCase(origin))) {
			return origin;
		}
		if ((referer != null) && (referer.startsWith("http"))) {
			int i = referer.indexOf("//");
			if (i != -1) {
				i = referer.indexOf('/', i + 2);
				if (i != -1) {
					return referer.substring(0, i);
				} else {
					return referer;
				}
			}
		}
		return null;
	}

}
