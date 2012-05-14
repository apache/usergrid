package org.usergrid.management.cassandra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.management.ManagementTestHelper;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.UserInfo;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.security.AuthPrincipalType;
import org.usergrid.security.tokens.TokenCategory;
import org.usergrid.utils.JsonUtils;

/**
 * @author zznate
 */
public class ManagementServiceTest {
	static Logger log = LoggerFactory.getLogger(ManagementServiceTest.class);
	static ManagementServiceImpl managementService;
	static ManagementTestHelper helper;
	// app-level data generated only once
	private static UserInfo adminUser;
	private static OrganizationInfo organization;
	private static UUID applicationId;

	@BeforeClass
	public static void setup() throws Exception {
		log.info("in setup");
		assertNull(helper);
		helper = new ManagementTestHelperImpl();
		helper.setup();
		managementService = (ManagementServiceImpl) helper
				.getManagementService();
		setupLocal();
	}

	public static void setupLocal() throws Exception {
		adminUser = managementService.createAdminUser("edanuff", "Ed Anuff",
				"ed@anuff.com", "test", false, false, false);
		organization = managementService.createOrganization("ed-organization",
				adminUser, false);
		applicationId = managementService.createApplication(
				organization.getUuid(), "ed-application");
	}

	@AfterClass
	public static void teardown() throws Exception {
		log.info("In teardown");
		helper.teardown();
	}

	@Test
	public void testGetTokenForPrincipalAdmin() throws Exception {
		String token = managementService.getTokenForPrincipal(
				TokenCategory.ACCESS, null, MANAGEMENT_APPLICATION_ID,
				AuthPrincipalType.ADMIN_USER, adminUser.getUuid());
		// ^ same as:
		// managementService.getAccessTokenForAdminUser(user.getUuid());
		assertNotNull(token);
		token = managementService.getTokenForPrincipal(TokenCategory.ACCESS,
				null, MANAGEMENT_APPLICATION_ID,
				AuthPrincipalType.APPLICATION_USER, adminUser.getUuid());
		// This works because ManagementService#getSecret takes the same code
		// path
		// on an OR for APP._USER as for ADMIN_USER
		// is ok technically as ADMIN_USER is a APP_USER to the admin app, but
		// should still
		// be stricter checking
		assertNotNull(token);
		// managementService.getTokenForPrincipal(appUuid, authPrincipal, pUuid,
		// salt, true);
	}

	@Test
	public void testGetTokenForPrincipalUser() throws Exception {
		// create a user
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("username", "edanuff");
		properties.put("email", "ed@anuff.com");

		Entity user = helper.getEntityManagerFactory()
				.getEntityManager(applicationId).create("user", properties);

		assertNotNull(user);
		String token = managementService.getTokenForPrincipal(
				TokenCategory.ACCESS, null, MANAGEMENT_APPLICATION_ID,
				AuthPrincipalType.APPLICATION_USER, user.getUuid());
		assertNotNull(token);
	}

	@Test
	public void testCountAdminUserAction() throws Exception {
		managementService.countAdminUserAction(adminUser, "login");

		EntityManager em = helper.getEntityManagerFactory().getEntityManager(
				MANAGEMENT_APPLICATION_ID);

		Map<String, Long> counts = em.getApplicationCounters();
		log.info(JsonUtils.mapToJsonString(counts));
		log.info(JsonUtils.mapToJsonString(em.getCounterNames()));
		assertNotNull(counts.get("admin_logins"));
		assertEquals(1, counts.get("admin_logins").intValue());
	}
}
