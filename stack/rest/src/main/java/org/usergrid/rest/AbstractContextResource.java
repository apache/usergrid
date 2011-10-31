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

	protected EntityManagerFactory emf;

	protected ServiceManagerFactory smf;

	protected ManagementService management;

	protected Properties properties;

	protected QueueManagerFactory qmf;

	public AbstractContextResource() {
	}

	public AbstractContextResource(AbstractContextResource parent) {
		this.parent = parent;
		uriInfo = parent.uriInfo;
		request = parent.request;
		sc = parent.sc;
		hc = parent.hc;
		cs = parent.cs;
		httpServletRequest = parent.httpServletRequest;
		emf = parent.emf;
		smf = parent.smf;
		management = parent.management;
		properties = parent.properties;
		qmf = parent.qmf;
	}

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

	public ManagementService getManagementService() {
		return management;
	}

	@Autowired
	public void setManagementService(ManagementService management) {
		this.management = management;
	}

	public Properties getProperties() {
		return properties;
	}

	@Autowired
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public QueueManagerFactory getQueueManagerFactory() {
		return qmf;
	}

	@Autowired
	public void setQueueManagerFactory(QueueManagerFactory qmf) {
		this.qmf = qmf;
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
