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
package org.usergrid.rest.filters;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

/**
 * @author tnine
 * 
 */
public class DefaultContentTypeFilter implements ContainerRequestFilter {

	public static final Logger logger = LoggerFactory
			.getLogger(DefaultContentTypeFilter.class);

	public static final String JSON_TYPE = "application/json; charset=utf-8";

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sun.jersey.spi.container.ContainerRequestFilter#filter(com.sun.jersey
	 * .spi.container.ContainerRequest)
	 */
	@Override
	public ContainerRequest filter(ContainerRequest request) {

		String accept_header = request.getHeaderValue(HttpHeaders.ACCEPT);
		String content_type = request.getHeaderValue(HttpHeaders.CONTENT_TYPE);
		logger.info("Accept: " + accept_header);
		logger.info("Content-type: " + content_type);

		if (accept_header == null || accept_header.contains("*/*")) {
			request.getRequestHeaders().putSingle(HttpHeaders.ACCEPT,
					MediaType.APPLICATION_JSON);
			request.getRequestHeaders().putSingle(HttpHeaders.CONTENT_TYPE,
					JSON_TYPE);
		}

		if (content_type == null || content_type.contains("*/*")
				|| content_type.contains("text/plain")) {
			request.getRequestHeaders().putSingle(HttpHeaders.CONTENT_TYPE,
					JSON_TYPE);
		}

		return request;
	}
}
