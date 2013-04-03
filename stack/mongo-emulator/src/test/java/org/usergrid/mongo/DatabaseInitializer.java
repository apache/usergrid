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
package org.usergrid.mongo;

import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.usergrid.management.ManagementService;
import org.usergrid.mq.QueueManagerFactory;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.cassandra.EntityManagerFactoryImpl;
import org.usergrid.persistence.cassandra.Setup;
import org.usergrid.services.ServiceManagerFactory;

public class DatabaseInitializer {

	private static final Logger logger = LoggerFactory
			.getLogger(DatabaseInitializer.class);

	protected EntityManagerFactory emf;

	protected ServiceManagerFactory smf;

	protected ManagementService management;

	protected Properties properties;

	protected QueueManagerFactory qmf;

	public DatabaseInitializer() {

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

	boolean databaseInitializationPerformed = false;

	public void init() {
		logger.info("Initializing server with Spring");

		// If we're running an embedded Cassandra, we always need to initialize
		// it since Hector wipes the data on startup.
		//

		if (databaseInitializationPerformed) {
			logger.info("Can only attempt to initialized database once per JVM process");
			return;
		}
		databaseInitializationPerformed = true;

		logger.info("Initializing Cassandra database");
		Map<String, String> properties = emf.getServiceProperties();
		if (properties != null) {
			logger.error("System properties are initialized, database is set up already.");
			return;
		}

		try {
			emf.setup();
		} catch (Exception e) {
			logger.error(
					"Unable to complete core database setup, possibly due to it being setup already",
					e);
		}

		try {
			management.setup();
		} catch (Exception e) {
			logger.error(
					"Unable to complete management database setup, possibly due to it being setup already",
					e);
		}

		logger.info("Usergrid schema setup");
	}

}
