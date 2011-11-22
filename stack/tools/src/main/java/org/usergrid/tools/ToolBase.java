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
package org.usergrid.tools;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import me.prettyprint.hector.testutils.EmbeddedServerHelper;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.usergrid.management.ManagementService;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.cassandra.EntityManagerFactoryImpl;
import org.usergrid.persistence.cassandra.Setup;
import org.usergrid.services.ServiceManagerFactory;

public abstract class ToolBase {

	private static final Logger logger = LoggerFactory.getLogger(ToolBase.class);

	EmbeddedServerHelper embedded = null;

	EntityManagerFactory emf;

	ServiceManagerFactory smf;

	ManagementService managementService;

	Properties properties;

	boolean use_remote = false;

	public void startTool(String[] args) {
		CommandLineParser parser = new GnuParser();
		CommandLine line = null;
		try {
			line = parser.parse(createOptions(), args);
		} catch (ParseException exp) {
			printCliHelp("Parsing failed.  Reason: " + exp.getMessage());
		}

		if (line == null) {
			return;
		}

		if (line.hasOption("remote")) {
			use_remote = true;
		}

		if (line.hasOption("host")) {
			use_remote = true;
			System.setProperty("cassandra.remote.url",
					line.getOptionValue("host"));
		}
		System.setProperty("cassandra.use_remote", Boolean.toString(use_remote));

		if (use_remote) {
			logger.info("Using remote Cassandra instance");
		} else {
			logger.info("Using local Cassandra instance");
		}

		try {
			runTool(line);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

	public void printCliHelp(String message) {
		System.out.println(message);
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java -jar usergrid-tools-0.0.1-SNAPSHOT.jar "
				+ getToolName(), createOptions());
		System.exit(-1);
	}

	public String getToolName() {
		return ClassUtils.getShortClassName(this.getClass());
	}

	public Options createOptions() {

		Options options = new Options();

		return options;
	}

	public void startEmbedded() throws Exception {
		// assertNotNull(client);

		String maven_opts = System.getenv("MAVEN_OPTS");
		logger.info("Maven options: " + maven_opts);

		logger.info("Starting Cassandra");
		embedded = new EmbeddedServerHelper();
		embedded.setup();
	}

	public void startSpring() {

		// copy("/testApplicationContext.xml", TMP);

		String[] locations = { "toolsApplicationContext.xml" };
		ApplicationContext ac = new ClassPathXmlApplicationContext(locations);

		AutowireCapableBeanFactory acbf = ac.getAutowireCapableBeanFactory();
		acbf.autowireBeanProperties(this,
				AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
		acbf.initializeBean(this, "testClient");

		assertNotNull(emf);
		assertTrue(
				"EntityManagerFactory is instance of EntityManagerFactoryImpl",
				emf instanceof EntityManagerFactoryImpl);

	}

	public void setupCassandra() throws Exception {

		Setup setup = ((EntityManagerFactoryImpl) emf).getSetup();
		logger.info("Setting up Usergrid schema");
		setup.setup();
		logger.info("Usergrid schema setup");

		setup.checkKeyspaces();

		logger.info("Setting up Usergrid management services");

		managementService.setup();

		logger.info("Usergrid management services setup");
	}

	public void teardownEmbedded() {
		logger.info("Stopping Cassandra");
		EmbeddedServerHelper.teardown();
	}

	@Autowired
	public void setEntityManagerFactory(EntityManagerFactory emf) {
		this.emf = emf;
	}

	@Autowired
	public void setServiceManagerFactory(ServiceManagerFactory smf) {
		this.smf = smf;
		logger.info("ManagementResource.setServiceManagerFactory");
	}

	@Autowired
	public void setManagementService(ManagementService managementService) {
		this.managementService = managementService;
	}

	@Autowired
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public abstract void runTool(CommandLine line) throws Exception;

}
