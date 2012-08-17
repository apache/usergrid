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
package org.usergrid.rest.utils;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;

public class CORSUtils {

	private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
	private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
	private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
	private static final String ACCESS_CONTROL_REQUEST_METHOD = "access-control-request-method";
	private static final String ACCESS_CONTROL_REQUEST_HEADERS = "access-control-request-headers";
	private static final String ORIGIN_HEADER = "origin";
	private static final String REFERER_HEADER = "referer";
	private static final String AUTHORIZATION_HEADER = "authorization";

	public static void allowAllOrigins(HttpServletRequest request,
			HttpServletResponse response) {

		if (request.getHeader(ACCESS_CONTROL_REQUEST_METHOD) != null) {
			@SuppressWarnings("unchecked")
			Enumeration<String> e = request
					.getHeaders(ACCESS_CONTROL_REQUEST_METHOD);
			while (e.hasMoreElements()) {
				String value = e.nextElement();
				response.addHeader(ACCESS_CONTROL_ALLOW_METHODS, value);
			}
		}

		if (request.getHeader(ACCESS_CONTROL_REQUEST_HEADERS) != null) {
			@SuppressWarnings("unchecked")
			Enumeration<String> e = request
					.getHeaders(ACCESS_CONTROL_REQUEST_HEADERS);
			while (e.hasMoreElements()) {
				String value = e.nextElement();
				response.addHeader(ACCESS_CONTROL_ALLOW_HEADERS, value);
			}
		}

		boolean origin_sent = false;
		boolean null_origin_received = false;
		if (request.getHeader(ORIGIN_HEADER) != null) {
			@SuppressWarnings("unchecked")
			Enumeration<String> e = request.getHeaders(ORIGIN_HEADER);
			while (e.hasMoreElements()) {
				String value = e.nextElement();
				if (value != null) {
					if ("null".equalsIgnoreCase(value)) {
						null_origin_received = true;
					} else {
						origin_sent = true;
						response.addHeader(ACCESS_CONTROL_ALLOW_ORIGIN, value);
					}
				}
			}
		}

		if (!origin_sent) {
			String origin = getOrigin(request);
			if (origin != null) {
				response.addHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
				response.addHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
			} else {
				if (!null_origin_received) {
					if (request.getHeaders(AUTHORIZATION_HEADER) == null) {
						response.addHeader(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
					}
				}
			}
		} else {
			response.addHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
		}

	}

	public static ContainerResponse allowAllOrigins(ContainerRequest request,
			ContainerResponse response) {

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
		if (request.getRequestHeaders().containsKey(ORIGIN_HEADER)) {
			for (String value : request.getRequestHeaders().get(ORIGIN_HEADER)) {
				if (!(value == null || "null".equalsIgnoreCase(value))) {
          origin_sent = true;
          response.getHttpHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN, value);
				}
			}
		}

		if (!origin_sent) {
			String origin = getOrigin(request);
			if (origin != null) {
				response.getHttpHeaders().add(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
				response.getHttpHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
			} else {
        if (!request.getRequestHeaders().containsKey(AUTHORIZATION_HEADER)) {
          response.getHttpHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        }
			}
		} else {
			response.getHttpHeaders().add(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
		}

		return response;
	}

	public static String getOrigin(String origin, String referer) {
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

	public static String getOrigin(HttpServletRequest request) {
		String origin = request.getHeader(ORIGIN_HEADER);
		String referer = request.getHeader(REFERER_HEADER);
		return getOrigin(origin, referer);
	}

	public static String getOrigin(ContainerRequest request) {
		String origin = request.getRequestHeaders().getFirst(ORIGIN_HEADER);
		String referer = request.getRequestHeaders().getFirst(REFERER_HEADER);
		return getOrigin(origin, referer);
	}

}
