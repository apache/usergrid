package org.usergrid.security.tokens;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.usergrid.utils.ClassUtils.cast;
import static org.usergrid.utils.MapUtils.hashMap;

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
import org.usergrid.management.cassandra.ManagementServiceImpl;
import org.usergrid.management.cassandra.ManagementTestHelperImpl;
import org.usergrid.security.AccessTokenInfo;
import org.usergrid.security.AuthPrincipalInfo;
import org.usergrid.security.AuthPrincipalType;

public class TokenServiceTest {

	static Logger log = LoggerFactory.getLogger(TokenServiceTest.class);
	static ManagementServiceImpl managementService;
	static ManagementTestHelper helper;
	static AccessTokenService tokenService;
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
		tokenService = helper.getAccessTokenService();
		setupLocal();
	}

	public static void setupLocal() throws Exception {
		adminUser = managementService.createAdminUser("edanuff", "Ed Anuff",
				"ed@anuff.com", "test", false, false, false);
		organization = managementService.createOrganization("ed-organization",
				adminUser);
		// TODO update to organizationName/applicationName
		applicationId = managementService.createApplication(
				organization.getUuid(), "ed-organization/ed-application");
	}

	@AfterClass
	public static void teardown() throws Exception {
		log.info("In teardown");
		helper.teardown();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testEmailConfirmToken() throws Exception {
		String tokenStr = tokenService.createAccessToken("email_confirm",
				(Map<String, Object>) cast(hashMap("email", "ed@anuff.com")
						.map("username", "edanuff")));

		AccessTokenInfo tokenInfo = tokenService.getAccessTokenInfo(tokenStr);

		assertEquals("email_confirm", tokenInfo.getType());
		assertEquals("ed@anuff.com", tokenInfo.getState().get("email"));
		assertEquals("edanuff", tokenInfo.getState().get("username"));
	}

	@Test
	public void testAdminPrincipalToken() throws Exception {
		String tokenStr = tokenService.createAccessToken(new AuthPrincipalInfo(
				AuthPrincipalType.ADMIN_USER, adminUser.getUuid(), null), null);

		AccessTokenInfo tokenInfo = tokenService.getAccessTokenInfo(tokenStr);

		assertEquals("access", tokenInfo.getType());
		assertEquals(adminUser.getUuid(), tokenInfo.getPrincipal().getUuid());
	}

}
