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
package org.usergrid.rest;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import net.tanesha.recaptcha.ReCaptcha;
import net.tanesha.recaptcha.ReCaptchaFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.usergrid.management.ManagementService;
import org.usergrid.mq.QueueManagerFactory;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.services.ServiceManagerFactory;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.core.ResourceContext;
import com.sun.jersey.spi.CloseableService;

public abstract class AbstractContextResource {

	protected AbstractContextResource parent;

	@Context
	protected UriInfo uriInfo;

	@Context
	protected Request request;

	@Context
	protected SecurityContext sc;

	@Context
	protected HttpContext hc;

	@Context
	protected CloseableService cs;

	@Context
	protected HttpServletRequest httpServletRequest;

	@Context
	protected ResourceContext resourceContext;

	@Autowired
	protected EntityManagerFactory emf;

	@Autowired
	protected ServiceManagerFactory smf;

	@Autowired
	protected ManagementService management;

	@Autowired
	protected Properties properties;

	@Autowired
	protected QueueManagerFactory qmf;

	public AbstractContextResource() {
	}

	public AbstractContextResource getParent() {
		return parent;
	}

	public void setParent(AbstractContextResource parent) {
		this.parent = parent;
	}

	public <T extends AbstractContextResource> T getSubResource(Class<T> t) {
		T subResource = resourceContext.getResource(t);
		subResource.setParent(this);
		return subResource;
	}

	public PathSegment getFirstPathSegment(String name) {
		if (name == null) {
			return null;
		}
		List<PathSegment> segments = uriInfo.getPathSegments();
		for (PathSegment segment : segments) {
			if (name.equals(segment.getPath())) {
				return segment;
			}
		}
		return null;
	}

	public boolean useReCaptcha() {
		return isNotBlank(properties.getProperty("usergrid.recaptcha.public"))
				&& isNotBlank(properties
						.getProperty("usergrid.recaptcha.private"));
	}

	public String getReCaptchaHtml() {
		if (!useReCaptcha()) {
			return "";
		}
		ReCaptcha c = ReCaptchaFactory.newReCaptcha(
				properties.getProperty("usergrid.recaptcha.public"),
				properties.getProperty("usergrid.recaptcha.private"), false);
		return c.createRecaptchaHtml(null, null);
	}

}
