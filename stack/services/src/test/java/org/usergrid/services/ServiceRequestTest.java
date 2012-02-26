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
package org.usergrid.services;

import static org.usergrid.persistence.cassandra.CassandraService.DEFAULT_APPLICATION_ID;
import static org.usergrid.services.ServiceParameter.filter;
import static org.usergrid.services.ServiceParameter.parameters;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ServiceRequestTest {

	private static final Logger logger = LoggerFactory
			.getLogger(ServiceRequestTest.class);

	@Test
	public void testPaths() throws Exception {

		UUID applicationId = DEFAULT_APPLICATION_ID;

		ServiceManagerFactory smf = new ServiceManagerFactory(null);
		ClassPathXmlApplicationContext ac = new ClassPathXmlApplicationContext();
		ac.refresh();
		smf.setApplicationContext(ac);
		ServiceManager services = smf.getServiceManager(applicationId);

		ServiceRequest path = null;

		path = services.newRequest(ServiceAction.GET,
				parameters("users", "bob"), null);
		// path = path.addSegment("users", "bob");
		logger.info("" + path.getParameters());

		Map<List<String>, List<String>> replaceParameters = new LinkedHashMap<List<String>, List<String>>();
		replaceParameters.put(Arrays.asList("users"),
				Arrays.asList("connecting", "users"));
		List<ServiceParameter> p = filter(path.getParameters(),
				replaceParameters);
		// path = path.addSegment("messages", "bob");
		logger.info("" + p);

		path = services.newRequest(ServiceAction.GET,
				parameters("users", UUID.randomUUID(), "messages"), null);
		logger.info("" + path.getParameters());

		logger.info("\\1");
		replaceParameters = new LinkedHashMap<List<String>, List<String>>();
		replaceParameters.put(Arrays.asList("users", "$id"),
				Arrays.asList("connecting", "\\1", "users"));
		p = filter(path.getParameters(), replaceParameters);
		logger.info("" + p);
	}
}
