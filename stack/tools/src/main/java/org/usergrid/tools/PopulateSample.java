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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.usergrid.services.ServiceParameter.parameters;
import static org.usergrid.services.ServicePayload.payload;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.UserInfo;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.entities.Activity;
import org.usergrid.services.ServiceAction;
import org.usergrid.services.ServiceManager;
import org.usergrid.services.ServiceRequest;
import org.usergrid.services.ServiceResults;

public class PopulateSample extends ToolBase {

	private static final Logger logger = LoggerFactory.getLogger(Export.class);

	@Override
	public Options createOptions() {

		Option useSpring = OptionBuilder.create("spring");

		Options options = new Options();
		options.addOption(useSpring);

		return options;
	}

	@Override
	public void runTool(CommandLine line) throws Exception {
		logger.info("Starting test...");
		startSpring();

		UserInfo user = managementService.createAdminUser("admin", "admin",
				"admin@ug.com", "none", false, false, false);

		logger.info("Creating organization: sample-organization");
		// management
		// .createOrganization("sample-organization", "sample@organization.com", "1234");
		OrganizationInfo organization = managementService.createOrganization("sample-organization",
				user);

		logger.info("creating application: testEntityManagerTest");
		UUID applicationId = managementService.createApplication(organization.getUuid(),
				"sample-application");

		ServiceManager sm = smf.getServiceManager(applicationId);

		EntityManager em = emf.getEntityManager(applicationId);

		// Create user
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("username", "edanuff");
		properties.put("email", "ed@anuff.com");
		properties.put("name", "Ed Anuff");

		Entity user1 = em.create("user", properties);

		// Create activity
		properties = Activity.newActivity(Activity.VERB_POST, null,
				"I ate a sammich", null, user1, null, "tweet", null, null)
				.getProperties();

		@SuppressWarnings("unused")
		Entity activity = testRequest(sm, ServiceAction.POST, 1, properties,
				"users", user1.getUuid(), "activities").getEntity();

		// Create another activity
		properties = Activity.newActivity(Activity.VERB_POST, null,
				"cool pic dude", null, user1, null, "tweet", null, null)
				.getProperties();

		activity = testRequest(sm, ServiceAction.POST, 1, properties, "users",
				user1.getUuid(), "activities").getEntity();

		// Create another user
		properties = new LinkedHashMap<String, Object>();
		properties.put("username", "justin");
		properties.put("email", "justin@gmail.com");
		properties.put("name", "Justin Clark");

		Entity user2 = em.create("user", properties);

		// Create activity
		properties = Activity.newActivity(Activity.VERB_POST, null,
				"ATT U-verse May payment", null, user2, null, "payment", null,
				null).getProperties();

		activity = testRequest(sm, ServiceAction.POST, 1, properties, "users",
				user2.getUuid(), "activities").getEntity();

		// Connections
		em.createConnection(user1, "workWith", user2);
	}

	public ServiceResults testRequest(ServiceManager sm, ServiceAction action,
			int expectedCount, Map<String, Object> properties, Object... params)
			throws Exception {
		ServiceRequest request = sm.newRequest(action, parameters(params),
				payload(properties));

		logger.info("Request: " + action + " " + request.toString());

		ServiceResults results = request.execute();
		assertNotNull(results);
		assertEquals(expectedCount, results.getEntities().size());

		return results;
	}

}
