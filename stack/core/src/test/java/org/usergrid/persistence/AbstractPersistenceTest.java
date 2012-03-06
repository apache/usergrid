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
package org.usergrid.persistence;

import static org.junit.Assert.assertNull;

import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.usergrid.mq.QueueManagerFactory;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.cassandra.PersistenceTestHelperImpl;
import org.usergrid.utils.JsonUtils;

/*
 import static org.testng.Assert.assertNull;
 import org.testng.annotations.AfterClass;
 import org.testng.annotations.BeforeClass;
 */

public abstract class AbstractPersistenceTest {

	public static final boolean USE_DEFAULT_APPLICATION = false;

	private static final Logger logger = LoggerFactory
			.getLogger(AbstractPersistenceTest.class);

	static PersistenceTestHelper helper;

	public AbstractPersistenceTest() {
		emf = helper.getEntityManagerFactory();
		qmf = helper.getMessageManagerFactory();
	}

	EntityManagerFactory emf;

	QueueManagerFactory qmf;

	/* @BeforeClass (groups="datastore") */
	@BeforeClass
	public static void setup() throws Exception {
		logger.info("setup");
		assertNull(helper);
		helper = new PersistenceTestHelperImpl();
		// helper.setClient(this);
		helper.setup();
	}

	/* @AfterClass /* (groups="datastore") */
	@AfterClass
	public static void teardown() throws Exception {
		logger.info("teardown");
		helper.teardown();
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return emf;
	}

	@Autowired
	public void setEntityManagerFactory(EntityManagerFactory emf) {
		this.emf = emf;
	}

	public QueueManagerFactory geQueueManagerFactory() {
		return qmf;
	}

	@Autowired
	public void setQueyeManagerFactory(QueueManagerFactory qmf) {
		this.qmf = qmf;
	}

	UUID dId = null;

	public UUID createApplication(String name) throws Exception {
		if (USE_DEFAULT_APPLICATION) {
			return CassandraService.DEFAULT_APPLICATION_ID;
		}
		return emf.createApplication(name);
	}

	public void dump(Object obj) {
		dump("Object", obj);
	}

	public void dump(String name, Object obj) {
		if (obj != null) {
			logger.info(name + ":\n" + JsonUtils.mapToFormattedJsonString(obj));
		}
	}
}
