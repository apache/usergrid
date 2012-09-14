package org.usergrid.management.cassandra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import me.prettyprint.cassandra.utils.TimeUUIDUtils;

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
import org.usergrid.persistence.entities.User;
import org.usergrid.security.AuthPrincipalType;
import org.usergrid.security.tokens.TokenCategory;
import org.usergrid.security.tokens.TokenService;
import org.usergrid.security.tokens.exceptions.InvalidTokenException;
import org.usergrid.utils.JsonUtils;
import org.usergrid.utils.UUIDUtils;

/**
 * @author zznate
 */
public class ManagementServiceTest {
	static Logger log = LoggerFactory.getLogger(ManagementServiceTest.class);
	static ManagementServiceImpl managementService;
	static TokenService tokenService;
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
        managementService = (ManagementServiceImpl) helper.getManagementService();
        tokenService = helper.getApplicationContext().getBean(TokenService.class);
		setupLocal();
	}

	public static void setupLocal() throws Exception {
		adminUser = managementService.createAdminUser("edanuff", "Ed Anuff",
				"ed@anuff.com", "test", false, false);
		organization = managementService.createOrganization("ed-organization",
				adminUser, true);
		applicationId = managementService.createApplication(
				organization.getUuid(), "ed-application")
            .getId();
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
	
	@Test
	public void deactivateUser() throws Exception{
	    
	    UUID uuid = UUIDUtils.newTimeUUID();
	    Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("username", "test"+uuid);
        properties.put("email", String.format("test%s@anuff.com", uuid));

        
        EntityManager em = helper.getEntityManagerFactory()
                .getEntityManager(applicationId);
        
        Entity entity = em.create("user", properties);

        assertNotNull(entity);
        
        User user = em.get(entity.getUuid(), User.class);
        
        assertFalse(user.activated());
        assertNull(user.getDeactivated());
        
        
        managementService.activateAppUser(applicationId, user.getUuid());
        
        user = em.get(entity.getUuid(), User.class);
        
        
        assertTrue(user.activated());
        assertNull(user.getDeactivated());
        
        //get a couple of tokens.  These shouldn't work after we deactive the user
        String token1 = managementService.getAccessTokenForAppUser(applicationId, user.getUuid());
        String token2 = managementService.getAccessTokenForAppUser(applicationId, user.getUuid());
        
        assertNotNull(tokenService.getTokenInfo(token1));
        assertNotNull(tokenService.getTokenInfo(token2));
        
        
        long startTime = System.currentTimeMillis();
	    
	    managementService.deactivateUser(applicationId, user.getUuid());
	    
	    long endTime = System.currentTimeMillis();
	    
	    user = em.get(entity.getUuid(), User.class);
        
        assertFalse(user.activated());
        assertNotNull(user.getDeactivated());
        
        assertTrue(startTime <= user.getDeactivated() && user.getDeactivated() <= endTime);
        
        boolean invalidTokenExcpetion = false;
        
        try{
            tokenService.getTokenInfo(token1);
        }catch(InvalidTokenException ite){
            invalidTokenExcpetion = true;
        }
        
        assertTrue(invalidTokenExcpetion);
        
        invalidTokenExcpetion = false;
        
        try{
            tokenService.getTokenInfo(token2);
        }catch(InvalidTokenException ite){
            invalidTokenExcpetion = true;
        }
        
        assertTrue(invalidTokenExcpetion);

        
        
	}
	
	@Test
    public void disableAdminUser() throws Exception{
        
        UUID uuid = UUIDUtils.newTimeUUID();
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("username", "test"+uuid);
        properties.put("email", String.format("test%s@anuff.com", uuid));

        
        EntityManager em = helper.getEntityManagerFactory()
                .getEntityManager(MANAGEMENT_APPLICATION_ID);
        
        Entity entity = em.create("user", properties);

        assertNotNull(entity);
        
        User user = em.get(entity.getUuid(), User.class);
        
        assertFalse(user.activated());
        assertNull(user.getDeactivated());
        
        
        managementService.activateAdminUser(user.getUuid());
        
        user = em.get(entity.getUuid(), User.class);
        
        
        assertTrue(user.activated());
        assertNull(user.getDeactivated());
        
        //get a couple of tokens.  These shouldn't work after we deactive the user
        String token1 = managementService.getAccessTokenForAdminUser(user.getUuid());
        String token2 = managementService.getAccessTokenForAdminUser(user.getUuid());
        
        assertNotNull(tokenService.getTokenInfo(token1));
        assertNotNull(tokenService.getTokenInfo(token2));
        
        
        
        managementService.disableAdminUser(user.getUuid());
        
        user = em.get(entity.getUuid(), User.class);
        
        assertTrue(user.disabled());
         
        boolean invalidTokenExcpetion = false;
        
        try{
            tokenService.getTokenInfo(token1);
        }catch(InvalidTokenException ite){
            invalidTokenExcpetion = true;
        }
        
        assertTrue(invalidTokenExcpetion);
        
        invalidTokenExcpetion = false;
        
        try{
            tokenService.getTokenInfo(token2);
        }catch(InvalidTokenException ite){
            invalidTokenExcpetion = true;
        }
        
        assertTrue(invalidTokenExcpetion);

        
        
    }
	
	@Test
	public void userTokenRevoke() throws Exception{
	    UUID userId = UUIDUtils.newTimeUUID();
	    
	    String token1 = managementService.getAccessTokenForAppUser(applicationId, userId);
        String token2 = managementService.getAccessTokenForAppUser(applicationId, userId);
        
        assertNotNull(tokenService.getTokenInfo(token1));
        assertNotNull(tokenService.getTokenInfo(token2));
        
        managementService.revokeAccessTokensForAppUser(applicationId, userId);
        
        boolean invalidTokenExcpetion = false;
        
        try{
            tokenService.getTokenInfo(token1);
        }catch(InvalidTokenException ite){
            invalidTokenExcpetion = true;
        }
        
        assertTrue(invalidTokenExcpetion);
        
        invalidTokenExcpetion = false;
        
        try{
            tokenService.getTokenInfo(token2);
        }catch(InvalidTokenException ite){
            invalidTokenExcpetion = true;
        }
        
        assertTrue(invalidTokenExcpetion);
        
      
        
	}
	
    @Test
    public void adminTokenRevoke() throws Exception {
        UUID userId = UUIDUtils.newTimeUUID();

        String token1 = managementService.getAccessTokenForAdminUser(userId);
        String token2 = managementService.getAccessTokenForAdminUser(userId);

        assertNotNull(tokenService.getTokenInfo(token1));
        assertNotNull(tokenService.getTokenInfo(token2));

        managementService.revokeAccessTokensForAdminUser(userId);

        boolean    invalidTokenExcpetion = false;
        
        try{
            tokenService.getTokenInfo(token1);
        }catch(InvalidTokenException ite){
            invalidTokenExcpetion = true;
        }
        
        assertTrue(invalidTokenExcpetion);
        
        invalidTokenExcpetion = false;
        
        try{
            tokenService.getTokenInfo(token2);
        }catch(InvalidTokenException ite){
            invalidTokenExcpetion = true;
        }
        
        assertTrue(invalidTokenExcpetion);

    }
}
