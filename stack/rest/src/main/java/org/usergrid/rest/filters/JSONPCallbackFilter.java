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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

@Component
public class JSONPCallbackFilter implements ContainerRequestFilter {

	private static final Logger logger = LoggerFactory
			.getLogger(JSONPCallbackFilter.class);

	@Context
	protected HttpServletRequest httpServletRequest;

	public JSONPCallbackFilter() {
		logger.info("JSONPCallbackFilter is installed");
	}

	@Override
	public ContainerRequest filter(ContainerRequest request) {
		String callback = httpServletRequest.getParameter("callback");
		if (isNotBlank(callback)) {
			request.getRequestHeaders().putSingle("Accept",
					"application/javascript");
		}
		return request;
	}

}
