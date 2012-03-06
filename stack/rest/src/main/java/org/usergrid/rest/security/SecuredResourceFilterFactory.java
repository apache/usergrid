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
package org.usergrid.rest.security;

import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.usergrid.rest.exceptions.SecurityException.mappableSecurityException;
import static org.usergrid.security.shiro.utils.SubjectUtils.isPermittedAccessToApplication;
import static org.usergrid.security.shiro.utils.SubjectUtils.isPermittedAccessToOrganization;
import static org.usergrid.security.shiro.utils.SubjectUtils.isUser;
import static org.usergrid.security.shiro.utils.SubjectUtils.loginApplicationGuest;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.ManagementService;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.Identifier;
import org.usergrid.rest.exceptions.SecurityException;
import org.usergrid.rest.security.annotations.RequireAdminUserAccess;
import org.usergrid.rest.security.annotations.RequireApplicationAccess;
import org.usergrid.rest.security.annotations.RequireOrganizationAccess;
import org.usergrid.rest.security.annotations.RequireSystemAccess;
import org.usergrid.security.shiro.utils.SubjectUtils;
import org.usergrid.services.ServiceManagerFactory;

import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;

@Component
public class SecuredResourceFilterFactory implements ResourceFilterFactory {

	private static final Logger logger = LoggerFactory
			.getLogger(SecuredResourceFilterFactory.class);

	private @Context
	UriInfo uriInfo;

	EntityManagerFactory emf;
	ServiceManagerFactory smf;

	Properties properties;

	ManagementService management;

	public SecuredResourceFilterFactory() {
		logger.info("SecuredResourceFilterFactory is installed");
	}

	@Autowired
	public void setEntityManagerFactory(EntityManagerFactory emf) {
		this.emf = emf;
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return emf;
	}

	@Autowired
	public void setServiceManagerFactory(ServiceManagerFactory smf) {
		this.smf = smf;
	}

	public ServiceManagerFactory getServiceManagerFactory() {
		return smf;
	}

	@Autowired
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	@Autowired
	public void setManagementService(ManagementService management) {
		this.management = management;
	}

	@Override
	public List<ResourceFilter> create(AbstractMethod am) {
		if (am.isAnnotationPresent(RequireApplicationAccess.class)) {
			return Collections
					.<ResourceFilter> singletonList(new ApplicationFilter());
		} else if (am.isAnnotationPresent(RequireOrganizationAccess.class)) {
			return Collections
					.<ResourceFilter> singletonList(new OrganizationFilter());
		} else if (am.isAnnotationPresent(RequireSystemAccess.class)) {
			return Collections
					.<ResourceFilter> singletonList(new SystemFilter());
		} else if (am.isAnnotationPresent(RequireAdminUserAccess.class)) {
			return Collections
					.<ResourceFilter> singletonList(new AdminUserFilter());
		}
		return null;
	}

	public abstract class AbstractFilter implements ResourceFilter,
			ContainerRequestFilter {
		public AbstractFilter() {
		}

		@Override
		public ContainerRequestFilter getRequestFilter() {
			return this;
		}

		@Override
		public ContainerResponseFilter getResponseFilter() {
			return null;
		}

		@Override
		public ContainerRequest filter(ContainerRequest request) {
			logger.info("Filtering " + request.getRequestUri().toString());

			if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
				logger.info("Skipping option request");
				return request;
			}

			MultivaluedMap<java.lang.String, java.lang.String> params = uriInfo
					.getPathParameters();
			logger.info("Params: " + params.keySet());

			authorize(request);
			return request;

		}

		public abstract void authorize(ContainerRequest request);

		public Identifier getApplicationIdentifier() {
			Identifier application = null;

			MultivaluedMap<java.lang.String, java.lang.String> pathParams = uriInfo
					.getPathParameters();
			String applicationIdStr = pathParams.getFirst("applicationId");
			if (isNotEmpty(applicationIdStr)) {
				application = Identifier.from(applicationIdStr);
			} else {
				String applicationName = pathParams.getFirst("applicationName");
				application = Identifier.fromName(applicationName);
			}

			return application;
		}

		public Identifier getOrganizationIdentifier() {
			Identifier organization = null;

			MultivaluedMap<java.lang.String, java.lang.String> pathParams = uriInfo
					.getPathParameters();
			String organizationIdStr = pathParams.getFirst("organizationId");
			if (isNotEmpty(organizationIdStr)) {
				organization = Identifier.from(organizationIdStr);
			} else {
				String organizationName = pathParams
						.getFirst("organizationName");
				organization = Identifier.fromName(organizationName);
			}

			return organization;
		}

		public Identifier getUserIdentifier() {

			MultivaluedMap<java.lang.String, java.lang.String> pathParams = uriInfo
					.getPathParameters();
			String userIdStr = pathParams.getFirst("userId");
			if (isNotEmpty(userIdStr)) {
				return Identifier.from(userIdStr);
			}
			String username = pathParams.getFirst("username");
			if (username != null) {
				return Identifier.fromName(username);
			}
			String email = pathParams.getFirst("email");
			if (email != null) {
				return Identifier.fromEmail(email);
			}
			return null;
		}

	}

	private class OrganizationFilter extends AbstractFilter {

		protected OrganizationFilter() {
		}

		@Override
		public void authorize(ContainerRequest request) {
			logger.info("OrganizationFilter.authorize");

			if (!isPermittedAccessToOrganization(getOrganizationIdentifier())) {
				throw mappableSecurityException("unauthorized",
						"No organization access authorized");
			}
		}

	}

	private class ApplicationFilter extends AbstractFilter {

		protected ApplicationFilter() {
		}

		@Override
		public void authorize(ContainerRequest request) {
			logger.info("ApplicationFilter.authorize");
			if (SubjectUtils.isAnonymous()) {
				ApplicationInfo application = null;
				try {
					application = management
							.getApplication(getApplicationIdentifier());
				} catch (Exception e) {
					e.printStackTrace();
				}
				loginApplicationGuest(application);
			}
			if (!isPermittedAccessToApplication(getApplicationIdentifier())) {
				throw mappableSecurityException("unauthorized",
						"No application access authorized");
			}
		}

	}

	public class SystemFilter extends AbstractFilter {
		public SystemFilter() {
		}

		@Override
		public void authorize(ContainerRequest request) {
			logger.info("SystemFilter.authorize");
			try {
				if (!request.isUserInRole("sysadmin")) {
					throw mappableSecurityException("unauthorized",
							"No system access authorized",
							SecurityException.REALM);
				}
			} catch (IllegalStateException e) {
				if ((request.getUserPrincipal() == null)
						|| !"sysadmin".equals(request.getUserPrincipal()
								.getName())) {
					throw mappableSecurityException("unauthorized",
							"No system access authorized",
							SecurityException.REALM);
				}
			}
		}

	}

	public class AdminUserFilter extends AbstractFilter {
		public AdminUserFilter() {
		}

		@Override
		public void authorize(ContainerRequest request) {
			logger.info("AdminUserFilter.authorize");
			if (!isUser(getUserIdentifier())) {
				throw mappableSecurityException("unauthorized",
						"No admin user access authorized");
			}
		}

	}
}
