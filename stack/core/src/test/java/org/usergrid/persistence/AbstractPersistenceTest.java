/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Core.
 * 
 * Usergrid Core is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Usergrid Core is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Usergrid Core. If not, see <http://www.gnu.org/licenses/>.
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
