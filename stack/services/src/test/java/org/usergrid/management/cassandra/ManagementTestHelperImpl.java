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
package org.usergrid.management.cassandra;

/*******************************************************************************
 * Copyright 2010,2011 Ed Anuff and Usergrid, all rights reserved.
 ******************************************************************************/

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
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;
import org.usergrid.management.ManagementService;
import org.usergrid.management.ManagementTestHelper;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.cassandra.EntityManagerFactoryImpl;

@Component
public class ManagementTestHelperImpl implements ManagementTestHelper {

	public static final boolean FORCE_QUIT = false;

	private static final Logger logger = Logger
			.getLogger(ManagementTestHelperImpl.class);

	// private static final String TMP = "tmp";

	// DatastoreTestClient client;

	EntityManagerFactory emf;

	javax.persistence.EntityManagerFactory jpaEmf;

	ManagementService management;

	Properties properties;

	public boolean forceQuit = FORCE_QUIT;

	public ManagementTestHelperImpl() {
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
		ApplicationContext ac = new ClassPathXmlApplicationContext(locations);

		AutowireCapableBeanFactory acbf = ac.getAutowireCapableBeanFactory();
		acbf.autowireBeanProperties(this,
				AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
		acbf.initializeBean(this, "testClient");

		assertNotNull(emf);
		assertTrue(
				"EntityManagerFactory is instance of EntityManagerFactoryImpl",
				emf instanceof EntityManagerFactoryImpl);

		emf.setup();

		management.setup();

	}

	@Override
	public void teardown() {
		logger.info("Stopping Cassandra");
		EmbeddedServerHelper.teardown();

		forceQuit();
	}

	public void forceQuit() {
		if (forceQuit) {
			logger.warn("\n\n\n******\n\nSystem.exit(0) to workaround Cassandra not stopping!\n\n******\n\n\n");
			System.exit(0);
		}
	}

	@Override
	public EntityManagerFactory getEntityManagerFactory() {
		return emf;
	}

	@Override
	@Autowired
	public void setEntityManagerFactory(EntityManagerFactory emf) {
		this.emf = emf;
		logger.info("ManagementTestHelperImpl.setEntityManagerFactory");
	}

	@Override
	public javax.persistence.EntityManagerFactory getJpaEntityManagerFactory() {
		return jpaEmf;
	}

	@Override
	public void setJpaEntityManagerFactory(
			javax.persistence.EntityManagerFactory jpaEmf) {
		this.jpaEmf = jpaEmf;
	}

	@Override
	public ManagementService getManagementService() {
		return management;
	}

	@Override
	@Autowired
	public void setManagementService(ManagementService management) {
		this.management = management;
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
