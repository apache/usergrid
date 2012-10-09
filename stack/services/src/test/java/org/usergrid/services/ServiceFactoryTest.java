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

import java.util.UUID;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.OrganizationOwnerInfo;

import baas.io.simple.SimpleService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/usergrid-test-context.xml")
public class ServiceFactoryTest extends AbstractServiceTest {

	private static final Logger logger = LoggerFactory
			.getLogger(ServiceFactoryTest.class);

	@Ignore
	@Test
	public void testServiceFactory() throws Exception {
		logger.info("test service factory");
	}
	
	@Test
	public void testPackagePrefixes() throws Exception {
		logger.info("test package prefixes");
		Assert.assertNotNull(properties);
		UUID applicationId = emf.createApplication("org", "app");
		ServiceManager sm = smf.getServiceManager(applicationId);
		Service service = sm.getService("simple");
		Assert.assertEquals("/simple", service.getServiceType());
		Assert.assertNotNull(service);
		Assert.assertEquals(SimpleService.class, service.getClass());
	}
}
