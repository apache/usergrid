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
package org.usergrid.persistence.cassandra;

/*
 import static org.testng.Assert.assertNotNull;
 import static org.testng.Assert.assertNull;
 */

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import me.prettyprint.hector.testutils.EmbeddedServerHelper;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;
import org.usergrid.mq.QueueManagerFactory;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.PersistenceTestHelper;

@Component
public class PersistenceTestHelperImpl implements PersistenceTestHelper {

	public static final boolean FORCE_QUIT = false;

	private static final Logger logger = Logger
			.getLogger(PersistenceTestHelperImpl.class);

	// private static final String TMP = "tmp";

	// DatastoreTestClient client;

	EntityManagerFactory emf;
	QueueManagerFactory mmf;
	Properties properties;

	public boolean forceQuit = FORCE_QUIT;

	public PersistenceTestHelperImpl() {

	}

	/*
	 * public DatastoreTestHelperImpl(DatastoreTestClient client) { this.client
	 * = client; }
	 * 
	 * public DatastoreTestClient getClient() { return client; }
	 * 
	 * public void setClient(DatastoreTestClient client) {
	 * assertNull(this.client); this.client = client; }
	 */

	ClassPathXmlApplicationContext ac = null;

	EmbeddedServerHelper embedded = null;

	@Override
	public void setup() throws Exception {
		// assertNotNull(client);

		String maven_opts = System.getenv("MAVEN_OPTS");
		logger.info("Maven options: " + maven_opts);

		logger.info("Starting Cassandra");
		embedded = new EmbeddedServerHelper();
		embedded.setup();

		// copy("/testApplicationContext.xml", TMP);

		String[] locations = { "testApplicationContext.xml" };
		ac = new ClassPathXmlApplicationContext(locations);

		AutowireCapableBeanFactory acbf = ac.getAutowireCapableBeanFactory();
		acbf.autowireBeanProperties(this,
				AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
		acbf.initializeBean(this, "testClient");

		assertNotNull(emf);
		assertTrue(
				"EntityManagerFactory is instance of EntityManagerFactoryImpl",
				emf instanceof EntityManagerFactoryImpl);

		Setup setup = ((EntityManagerFactoryImpl) emf).getSetup();

		logger.info("Setting up Usergrid schema");
		setup.setup();
		logger.info("Usergrid schema setup");
		setup.checkKeyspaces();

	}

	@Override
	public void teardown() {
		logger.info("Stopping Cassandra");
		EmbeddedServerHelper.teardown();
		if (ac != null) {
			ac.close();
		}
		forceQuit();
	}

	public void forceQuit() {
		if (forceQuit) {
			logger.warn("\n\n\n******\n\nSystem.exit(0) to workaround Cassandra not stopping!\n\n******\n\n\n");
			System.exit(0);
		}
	}

	@Override
	@Autowired
	public void setEntityManagerFactory(EntityManagerFactory emf) {
		this.emf = emf;
	}

	@Override
	public EntityManagerFactory getEntityManagerFactory() {
		return emf;
	}

	@Override
	public QueueManagerFactory getMessageManagerFactory() {
		return mmf;
	}

	@Override
	@Autowired
	public void setMessageManagerFactory(QueueManagerFactory mmf) {
		this.mmf = mmf;
	}

	@Override
	public Properties getProperties() {
		return properties;
	}

	@Override
	@Autowired
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public boolean isForceQuit() {
		return forceQuit;
	}

	public void setForceQuit(boolean forceQuit) {
		this.forceQuit = forceQuit;
	}

}
