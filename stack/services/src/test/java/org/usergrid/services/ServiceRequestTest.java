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
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.usergrid.cassandra.CassandraRunner;

@RunWith(CassandraRunner.class)
public class ServiceRequestTest {

	private static final Logger logger = LoggerFactory
			.getLogger(ServiceRequestTest.class);

	@Test
	public void testPaths() throws Exception {

		UUID applicationId = DEFAULT_APPLICATION_ID;

		ServiceManagerFactory smf = CassandraRunner.getBean(ServiceManagerFactory.class);

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
