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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;
import org.usergrid.management.ManagementService;
import org.usergrid.management.ManagementTestHelper;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.cassandra.EntityManagerFactoryImpl;
import org.usergrid.security.tokens.TokenService;

@Component
public class ManagementTestHelperImpl implements ManagementTestHelper {

	public static final boolean FORCE_QUIT = false;

	private static final Logger logger = LoggerFactory
			.getLogger(ManagementTestHelperImpl.class);

	// private static final String TMP = "tmp";

	// DatastoreTestClient client;

	EntityManagerFactory emf;

	javax.persistence.EntityManagerFactory jpaEmf;

	ManagementService management;

	TokenService tokens;

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

	@Override
	public TokenService getAccessTokenService() {
		return tokens;
	}

	@Override
	@Autowired
	public void setAccessTokenService(TokenService tokens) {
		this.tokens = tokens;
	}

	public boolean isForceQuit() {
		return forceQuit;
	}

	public void setForceQuit(boolean forceQuit) {
		this.forceQuit = forceQuit;
	}

}
