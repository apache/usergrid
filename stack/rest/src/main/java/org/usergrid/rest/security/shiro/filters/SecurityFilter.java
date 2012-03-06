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
package org.usergrid.rest.security.shiro.filters;

import static org.usergrid.utils.StringUtils.stringOrSubstringAfterFirst;
import static org.usergrid.utils.StringUtils.stringOrSubstringBeforeFirst;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.usergrid.management.ManagementService;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.services.ServiceManagerFactory;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

public abstract class SecurityFilter implements ContainerRequestFilter {

	public static final String AUTH_OAUTH_2_ACCESS_TOKEN_TYPE = "BEARER";
	public static final String AUTH_BASIC_TYPE = "BASIC";
	public static final String AUTH_OAUTH_1_TYPE = "OAUTH";

	EntityManagerFactory emf;
	ServiceManagerFactory smf;
	Properties properties;
	ManagementService management;

	@Context
	UriInfo uriInfo;

	@Context
	HttpContext hc;

	public EntityManagerFactory getEntityManagerFactory() {
		return emf;
	}

	@Autowired
	public void setEntityManagerFactory(EntityManagerFactory emf) {
		this.emf = emf;
	}

	public ServiceManagerFactory getServiceManagerFactory() {
		return smf;
	}

	@Autowired
	public void setServiceManagerFactory(ServiceManagerFactory smf) {
		this.smf = smf;
	}

	public Properties getProperties() {
		return properties;
	}

	@Autowired
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public ManagementService getManagementService() {
		return management;
	}

	@Autowired
	public void setManagementService(ManagementService management) {
		this.management = management;
	}

	public static Map<String, String> getAuthTypes(ContainerRequest request) {
		String auth_header = request.getHeaderValue(HttpHeaders.AUTHORIZATION);
		if (auth_header == null) {
			return null;
		}

		String[] auth_list = StringUtils.split(auth_header, ',');
		if (auth_list == null) {
			return null;
		}
		Map<String, String> auth_types = new LinkedHashMap<String, String>();
		for (String auth : auth_list) {
			auth = auth.trim();
			String type = stringOrSubstringBeforeFirst(auth, ' ').toUpperCase();
			String token = stringOrSubstringAfterFirst(auth, ' ');
			auth_types.put(type, token);
		}
		return auth_types;
	}

}
