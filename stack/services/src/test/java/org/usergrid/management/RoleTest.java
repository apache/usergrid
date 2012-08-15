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
package org.usergrid.management;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.shiro.subject.Subject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.management.cassandra.ManagementTestHelperImpl;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.cassandra.EntityManagerFactoryImpl;
import org.usergrid.persistence.entities.User;
import org.usergrid.security.shiro.PrincipalCredentialsToken;
import org.usergrid.security.shiro.utils.SubjectUtils;
import org.usergrid.services.ServiceManagerFactory;

public class RoleTest {

	private static final Logger logger = LoggerFactory
			.getLogger(RoleTest.class);

	static ManagementService management;

	static ManagementTestHelper helper;

	static EntityManagerFactoryImpl emf;
	static ServiceManagerFactory smf;

	@BeforeClass
	public static void setup() throws Exception {
		logger.info("setup");
		assertNull(helper);
		helper = new ManagementTestHelperImpl();
		// helper.setClient(this);
		helper.setup();
		management = helper.getManagementService();
		emf = (EntityManagerFactoryImpl) helper.getEntityManagerFactory();
		smf = new ServiceManagerFactory(emf);
		smf.setApplicationContext(helper.getApplicationContext());
	}

	@AfterClass
	public static void teardown() throws Exception {
		logger.info("teardown");
		helper.teardown();
	}

	@Test
	public void testRoleInactivity() throws Exception {

		OrganizationOwnerInfo ooi = management.createOwnerAndOrganization(
				"ed-organization", "edanuff", "Ed Anuff", "ed@anuff.com",
				"test", true, false);

		OrganizationInfo organization = ooi.getOrganization();

		UUID applicationId = management.createApplication(
				organization.getUuid(), "test-app")
            .getId();
		EntityManager em = emf.getEntityManager(applicationId);

		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("username", "edanuff");
		properties.put("email", "ed@anuff.com");
		properties.put("activated", true);
		User user = em.create(User.ENTITY_TYPE, User.class, properties);

		em.createRole("logged-in", "Logged In", 1000);
		em.addUserToRole(user.getUuid(), "logged-in");

		String accessToken = management.getAccessTokenForAppUser(applicationId,
				user.getUuid());

		UserInfo user_info = management.getAppUserFromAccessToken(accessToken);

		PrincipalCredentialsToken token = PrincipalCredentialsToken
				.getFromAppUserInfoAndAccessToken(user_info, accessToken);

		Subject subject = SubjectUtils.getSubject();
		subject.login(token);

		subject.checkRole("application-role:" + applicationId + ":logged-in");

		logger.info("Has role \"logged-in\"");

		Thread.sleep(1000);

		subject.login(token);

		assertFalse(subject.hasRole("application-role:" + applicationId
				+ ":logged-in"));

		logger.info("Doesn't have role \"logged-in\"");

	}

}
