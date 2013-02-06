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
package org.usergrid.python;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JythonTest {
	private static final Logger logger = LoggerFactory.getLogger(JythonTest.class);

	@Test
	public void testJythonObjectFactory() throws Exception {
		logger.info("Jython tests disabled");

		/*
		 * EntityRef e = (EntityRef) PythonUtils.createObject(EntityRef.class,
		 * "test.test", "Test");
		 * 
		 * logger.info(e.getType()); logger.info(e.getId());
		 */
	}

	@Test
	public void testJythonBasedService() throws Exception {
		logger.info("Jython tests disabled");

		/*
		 * Service s = (Service) PythonUtils.createObject(Service.class,
		 * "pyusergrid.services.pytest.PytestService", "PytestService");
		 * 
		 * logger.info(s.getEntityType()); logger.info(s.getEntityClass());
		 */
	}

}
