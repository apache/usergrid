package org.usergrid.management.cassandra;

import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.usergrid.persistence.Schema.DICTIONARY_CREDENTIALS;
import static org.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.cassandra.CassandraRunner;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.UserInfo;
import org.usergrid.persistence.CredentialsInfo;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.entities.User;
import org.usergrid.security.AuthPrincipalType;
import org.usergrid.security.crypto.command.Md5HashCommand;
import org.usergrid.security.crypto.command.Sha1HashCommand;
import org.usergrid.security.tokens.TokenCategory;
import org.usergrid.security.tokens.TokenService;
import org.usergrid.security.tokens.exceptions.InvalidTokenException;
import org.usergrid.test.ShiroHelperRunner;
import org.usergrid.utils.JsonUtils;
import org.usergrid.utils.UUIDUtils;

import com.usergrid.count.SimpleBatcher;

/**
 * @author zznate
 */
@RunWith(ShiroHelperRunner.class)
public class ManagementServiceTest {
  static Logger log = LoggerFactory.getLogger(ManagementServiceTest.class);
  static ManagementServiceImpl managementService;
  static TokenService tokenService;
  static EntityManagerFactory emf;

  // app-level data generated only once
  private static UserInfo adminUser;
  private static OrganizationInfo organization;
  private static UUID applicationId;

  @BeforeClass
  public static void setup() throws Exception {
    log.info("in setup");
    managementService = CassandraRunner.getBean(ManagementServiceImpl.class);
    tokenService = CassandraRunner.getBean(TokenService.class);
    emf = CassandraRunner.getBean(EntityManagerFactory.class);
    setupLocal();
  }

  public static void setupLocal() throws Exception {
    adminUser = managementService.createAdminUser("edanuff", "Ed Anuff", "ed@anuff.com", "test", false, false);
    organization = managementService.createOrganization("ed-organization", adminUser, true);
    applicationId = managementService.createApplication(organization.getUuid(), "ed-application").getId();
  }

  @Test
  public void testGetTokenForPrincipalAdmin() throws Exception {
    String token = managementService.getTokenForPrincipal(TokenCategory.ACCESS, null, MANAGEMENT_APPLICATION_ID,
        AuthPrincipalType.ADMIN_USER, adminUser.getUuid(), 0);
    // ^ same as:
    // managementService.getAccessTokenForAdminUser(user.getUuid());
    assertNotNull(token);
    token = managementService.getTokenForPrincipal(TokenCategory.ACCESS, null, MANAGEMENT_APPLICATION_ID,
        AuthPrincipalType.APPLICATION_USER, adminUser.getUuid(), 0);
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

    Entity user = emf.getEntityManager(applicationId).create("user", properties);

    assertNotNull(user);
    String token = managementService.getTokenForPrincipal(TokenCategory.ACCESS, null, MANAGEMENT_APPLICATION_ID,
        AuthPrincipalType.APPLICATION_USER, user.getUuid(), 0);
    assertNotNull(token);
  }

  @Test
  public void testCountAdminUserAction() throws Exception {
    SimpleBatcher batcher = CassandraRunner.getBean(SimpleBatcher.class);

    batcher.setBlockingSubmit(true);

    managementService.countAdminUserAction(adminUser, "login");

    EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);

    Map<String, Long> counts = em.getApplicationCounters();
    log.info(JsonUtils.mapToJsonString(counts));
    log.info(JsonUtils.mapToJsonString(em.getCounterNames()));
    assertNotNull(counts.get("admin_logins"));
    assertEquals(1, counts.get("admin_logins").intValue());
  }

  @Test
  public void deactivateUser() throws Exception {

    UUID uuid = UUIDUtils.newTimeUUID();
    Map<String, Object> properties = new LinkedHashMap<String, Object>();
    properties.put("username", "test" + uuid);
    properties.put("email", String.format("test%s@anuff.com", uuid));

    EntityManager em = emf.getEntityManager(applicationId);

    Entity entity = em.create("user", properties);

    assertNotNull(entity);

    User user = em.get(entity.getUuid(), User.class);

    assertFalse(user.activated());
    assertNull(user.getDeactivated());

    managementService.activateAppUser(applicationId, user.getUuid());

    user = em.get(entity.getUuid(), User.class);

    assertTrue(user.activated());
    assertNull(user.getDeactivated());

    // get a couple of tokens. These shouldn't work after we deactive the user
    String token1 = managementService.getAccessTokenForAppUser(applicationId, user.getUuid(), 0);
    String token2 = managementService.getAccessTokenForAppUser(applicationId, user.getUuid(), 0);

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

    try {
      tokenService.getTokenInfo(token1);
    } catch (InvalidTokenException ite) {
      invalidTokenExcpetion = true;
    }

    assertTrue(invalidTokenExcpetion);

    invalidTokenExcpetion = false;

    try {
      tokenService.getTokenInfo(token2);
    } catch (InvalidTokenException ite) {
      invalidTokenExcpetion = true;
    }

    assertTrue(invalidTokenExcpetion);

  }

  @Test
  public void disableAdminUser() throws Exception {

    UUID uuid = UUIDUtils.newTimeUUID();
    Map<String, Object> properties = new LinkedHashMap<String, Object>();
    properties.put("username", "test" + uuid);
    properties.put("email", String.format("test%s@anuff.com", uuid));

    EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);

    Entity entity = em.create("user", properties);

    assertNotNull(entity);

    User user = em.get(entity.getUuid(), User.class);

    assertFalse(user.activated());
    assertNull(user.getDeactivated());

    managementService.activateAdminUser(user.getUuid());

    user = em.get(entity.getUuid(), User.class);

    assertTrue(user.activated());
    assertNull(user.getDeactivated());

    // get a couple of tokens. These shouldn't work after we deactive the user
    String token1 = managementService.getAccessTokenForAdminUser(user.getUuid(), 0);
    String token2 = managementService.getAccessTokenForAdminUser(user.getUuid(), 0);

    assertNotNull(tokenService.getTokenInfo(token1));
    assertNotNull(tokenService.getTokenInfo(token2));

    managementService.disableAdminUser(user.getUuid());

    user = em.get(entity.getUuid(), User.class);

    assertTrue(user.disabled());

    boolean invalidTokenExcpetion = false;

    try {
      tokenService.getTokenInfo(token1);
    } catch (InvalidTokenException ite) {
      invalidTokenExcpetion = true;
    }

    assertTrue(invalidTokenExcpetion);

    invalidTokenExcpetion = false;

    try {
      tokenService.getTokenInfo(token2);
    } catch (InvalidTokenException ite) {
      invalidTokenExcpetion = true;
    }

    assertTrue(invalidTokenExcpetion);

  }

  @Test
  public void userTokensRevoke() throws Exception {
    UUID userId = UUIDUtils.newTimeUUID();

    String token1 = managementService.getAccessTokenForAppUser(applicationId, userId, 0);
    String token2 = managementService.getAccessTokenForAppUser(applicationId, userId, 0);

    assertNotNull(tokenService.getTokenInfo(token1));
    assertNotNull(tokenService.getTokenInfo(token2));

    managementService.revokeAccessTokensForAppUser(applicationId, userId);

    boolean invalidTokenExcpetion = false;

    try {
      tokenService.getTokenInfo(token1);
    } catch (InvalidTokenException ite) {
      invalidTokenExcpetion = true;
    }

    assertTrue(invalidTokenExcpetion);

    invalidTokenExcpetion = false;

    try {
      tokenService.getTokenInfo(token2);
    } catch (InvalidTokenException ite) {
      invalidTokenExcpetion = true;
    }

    assertTrue(invalidTokenExcpetion);
  }

  @Test
  public void userTokenRevoke() throws Exception {
    EntityManager em = emf.getEntityManager(applicationId);

    Map<String, Object> properties = new LinkedHashMap<String, Object>();
    properties.put("username", "realbeast");
    properties.put("email", "sungju@softwaregeeks.org");

    Entity user = em.create("user", properties);
    assertNotNull(user);

    UUID userId = user.getUuid();

    String token1 = managementService.getAccessTokenForAppUser(applicationId, userId, 0);
    String token2 = managementService.getAccessTokenForAppUser(applicationId, userId, 0);

    assertNotNull(tokenService.getTokenInfo(token1));
    assertNotNull(tokenService.getTokenInfo(token2));

    managementService.revokeAccessTokenForAppUser(token1);

    boolean invalidToken1Excpetion = false;

    try {
      tokenService.getTokenInfo(token1);
    } catch (InvalidTokenException ite) {
      invalidToken1Excpetion = true;
    }

    assertTrue(invalidToken1Excpetion);

    boolean invalidToken2Excpetion = true;

    try {
      tokenService.getTokenInfo(token2);
    } catch (InvalidTokenException ite) {
      invalidToken2Excpetion = false;
    }

    assertTrue(invalidToken2Excpetion);
  }

  @Test
  public void adminTokensRevoke() throws Exception {
    UUID userId = UUIDUtils.newTimeUUID();

    String token1 = managementService.getAccessTokenForAdminUser(userId, 0);
    String token2 = managementService.getAccessTokenForAdminUser(userId, 0);

    assertNotNull(tokenService.getTokenInfo(token1));
    assertNotNull(tokenService.getTokenInfo(token2));

    managementService.revokeAccessTokensForAdminUser(userId);

    boolean invalidTokenExcpetion = false;

    try {
      tokenService.getTokenInfo(token1);
    } catch (InvalidTokenException ite) {
      invalidTokenExcpetion = true;
    }

    assertTrue(invalidTokenExcpetion);

    invalidTokenExcpetion = false;

    try {
      tokenService.getTokenInfo(token2);
    } catch (InvalidTokenException ite) {
      invalidTokenExcpetion = true;
    }

    assertTrue(invalidTokenExcpetion);
  }

  @Test
  public void adminTokenRevoke() throws Exception {
    UUID userId = adminUser.getUuid();

    String token1 = managementService.getAccessTokenForAdminUser(userId, 0);
    String token2 = managementService.getAccessTokenForAdminUser(userId, 0);

    assertNotNull(tokenService.getTokenInfo(token1));
    assertNotNull(tokenService.getTokenInfo(token2));

    managementService.revokeAccessTokenForAdminUser(userId, token1);

    boolean invalidToken1Excpetion = false;

    try {
      tokenService.getTokenInfo(token1);
    } catch (InvalidTokenException ite) {
      invalidToken1Excpetion = true;
    }

    assertTrue(invalidToken1Excpetion);

    boolean invalidToken2Excpetion = true;

    try {
      tokenService.getTokenInfo(token2);
    } catch (InvalidTokenException ite) {
      invalidToken2Excpetion = false;
    }

    assertTrue(invalidToken2Excpetion);
  }

  @Test
  public void superUserGetOrganizationsPage() throws Exception {
    // create 15 orgs
    for (int x = 0; x < 15; x++) {
      managementService.createOrganization("super-user-org-" + x, adminUser, true);
    }
    // should be 17 total
    assertEquals(16, managementService.getOrganizations().size());
    List<OrganizationInfo> orgs = managementService.getOrganizations(null, 10);
    assertEquals(10, orgs.size());
    UUID val = orgs.get(9).getUuid();
    orgs = managementService.getOrganizations(val, 10);
    assertEquals(7, orgs.size());
    assertEquals(val, orgs.get(0).getUuid());
  }

  @Test
  public void authenticateAdmin() throws Exception {

    String username = "tnine";
    String password = "test";

    UserInfo adminUser = managementService.createAdminUser(username, "Todd Nine", UUID.randomUUID() + "@apigee.com",
        password, false, false);

    UserInfo authedUser = managementService.verifyAdminUserPasswordCredentials(username, password);

    assertEquals(adminUser.getUuid(), authedUser.getUuid());

    authedUser = managementService.verifyAdminUserPasswordCredentials(adminUser.getEmail(), password);

    assertEquals(adminUser.getUuid(), authedUser.getUuid());

    authedUser = managementService.verifyAdminUserPasswordCredentials(adminUser.getUuid().toString(), password);

    assertEquals(adminUser.getUuid(), authedUser.getUuid());

  }
  
  
  /**
   * Test we can change the password if it's hashed with sha1
   * @throws Exception 
   */
  @Test
  public void testAdminPasswordChangeShaType() throws Exception{
    String username = "testAdminPasswordChangeShaType";
    String password = "test";
   
    
    User user = new User();
    user.setActivated(true);
    user.setUsername(username);
    
    EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
    
    User storedUser = em.create(user);
    
    
    UUID userId = storedUser.getUuid();
    
    //set the password in the sha1 format
    CredentialsInfo info = new CredentialsInfo();
    info.setRecoverable(false);
    info.setEncrypted(true);
    
    
    Sha1HashCommand command = new Sha1HashCommand();
    byte[] hashed =  command.hash(password.getBytes("UTF-8"), info, userId, MANAGEMENT_APPLICATION_ID);
    
    info.setSecret(encodeBase64URLSafeString(hashed));
    info.setCipher(command.getName());
    
   
    em.addToDictionary(storedUser, DICTIONARY_CREDENTIALS, "password", info);
    
    
    //verify authorization works
    User authedUser = managementService.verifyAppUserPasswordCredentials(MANAGEMENT_APPLICATION_ID, username, password);

    assertEquals(userId, authedUser.getUuid());
    
    //test we can change the password
    String newPassword = "test2";
    
    managementService.setAppUserPassword(MANAGEMENT_APPLICATION_ID, userId, password, newPassword);
    
    //verify authorization works
    authedUser = managementService.verifyAppUserPasswordCredentials(MANAGEMENT_APPLICATION_ID, username, newPassword);

    assertEquals(userId, authedUser.getUuid());
  }
  
  /**
   * Test we can change the password if it's hashed with md5 then sha1 
   * @throws Exception 
   */
  @Test
  public void testAdminPasswordChangeMd5ShaType() throws Exception{
    String username = "testAdminPasswordChangeMd5ShaType";
    String password = "test";
   
    
    User user = new User();
    user.setActivated(true);
    user.setUsername(username);
    
    EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
    
    User storedUser = em.create(user);
    
    
    UUID userId = storedUser.getUuid();
    
    //set the password in the sha1 format
    
    //set the password in the sha1 format
    CredentialsInfo info = new CredentialsInfo();
    info.setRecoverable(false);
    info.setEncrypted(true);
    
    
    Md5HashCommand md5 = new Md5HashCommand();
    
    Sha1HashCommand sha1 = new Sha1HashCommand();
    
    byte[] hashed =  md5.hash(password.getBytes("UTF-8"), info, userId, MANAGEMENT_APPLICATION_ID);
    hashed =  sha1.hash(hashed, info, userId, MANAGEMENT_APPLICATION_ID);
    
    info.setSecret(encodeBase64URLSafeString(hashed));
    //set the final cipher to sha1
    info.setCipher(sha1.getName());
    //set the next hash type to md5
    info.setHashType(md5.getName());
    
   
    em.addToDictionary(storedUser, DICTIONARY_CREDENTIALS, "password", info);
    
    
    //verify authorization works
    User authedUser = managementService.verifyAppUserPasswordCredentials(MANAGEMENT_APPLICATION_ID, username, password);

    assertEquals(userId, authedUser.getUuid());
    
    //test we can change the password
    String newPassword = "test2";
    
    managementService.setAppUserPassword(MANAGEMENT_APPLICATION_ID, userId, password, newPassword);
    
    //verify authorization works
    authedUser = managementService.verifyAppUserPasswordCredentials(MANAGEMENT_APPLICATION_ID, username, newPassword);

    assertEquals(userId, authedUser.getUuid());
  }
  
  @Test
  public void authenticateUser() throws Exception {

    String username = "tnine";
    String password = "test";
    String orgName = "autneticateUser";
    String appName = "authenticateUser";
    
    UUID appId = emf.createApplication(orgName, appName);
    
    User user = new User();
    user.setActivated(true);
    user.setUsername(username);
    
    EntityManager em = emf.getEntityManager(appId);
    
    User storedUser = em.create(user);
    
    
    UUID userId = storedUser.getUuid();
    
    //set the password
    managementService.setAppUserPassword(appId, userId, password);
    
    //verify authorization works
    User authedUser = managementService.verifyAppUserPasswordCredentials(appId, username, password);

    assertEquals(userId, authedUser.getUuid());
    
    //test we can change the password
    String newPassword = "test2";
    
    managementService.setAppUserPassword(appId, userId, password, newPassword);
    
    //verify authorization works
    authedUser = managementService.verifyAppUserPasswordCredentials(appId, username, newPassword);


  }
  
  /**
   * Test we can change the password if it's hashed with sha1
   * @throws Exception 
   */
  @Test
  public void testAppUserPasswordChangeShaType() throws Exception{
    String username = "tnine";
    String password = "test";
    String orgName = "testAppUserPasswordChangeShaType";
    String appName = "testAppUserPasswordChangeShaType";
    
    UUID appId = emf.createApplication(orgName, appName);
    
    User user = new User();
    user.setActivated(true);
    user.setUsername(username);
    
    EntityManager em = emf.getEntityManager(appId);
    
    User storedUser = em.create(user);
    
    
    UUID userId = storedUser.getUuid();
    
    //set the password in the sha1 format
    CredentialsInfo info = new CredentialsInfo();
    info.setRecoverable(false);
    info.setEncrypted(true);
    
    
    Sha1HashCommand command = new Sha1HashCommand();
    byte[] hashed =  command.hash(password.getBytes("UTF-8"), info, userId, appId);
    
    info.setSecret(encodeBase64URLSafeString(hashed));
    info.setCipher(command.getName());
    
   
    em.addToDictionary(storedUser, DICTIONARY_CREDENTIALS, "password", info);
    
    
    //verify authorization works
    User authedUser = managementService.verifyAppUserPasswordCredentials(appId, username, password);

    assertEquals(userId, authedUser.getUuid());
    
    //test we can change the password
    String newPassword = "test2";
    
    managementService.setAppUserPassword(appId, userId, password, newPassword);
    
    //verify authorization works
    authedUser = managementService.verifyAppUserPasswordCredentials(appId, username, newPassword);

    assertEquals(userId, authedUser.getUuid());

  }
  
  /**
   * Test we can change the password if it's hashed with md5 then sha1 
   * @throws Exception 
   */
  @Test
  public void testAppUserPasswordChangeMd5ShaType() throws Exception{
    String username = "tnine";
    String password = "test";
    String orgName = "testAppUserPasswordChangeMd5ShaType";
    String appName = "testAppUserPasswordChangeMd5ShaType";
    
    UUID appId = emf.createApplication(orgName, appName);
    
    User user = new User();
    user.setActivated(true);
    user.setUsername(username);
    
    EntityManager em = emf.getEntityManager(appId);
    
    User storedUser = em.create(user);
    
    
    UUID userId = storedUser.getUuid();
    
    //set the password in the sha1 format
    CredentialsInfo info = new CredentialsInfo();
    info.setRecoverable(false);
    info.setEncrypted(true);
    
    
    Md5HashCommand md5 = new Md5HashCommand();
    
    Sha1HashCommand sha1 = new Sha1HashCommand();
    
    byte[] hashed =  md5.hash(password.getBytes("UTF-8"), info, userId, appId);
    hashed =  sha1.hash(hashed, info, userId, appId);
    
    info.setSecret(encodeBase64URLSafeString(hashed));
    //set the final cipher to sha1
    info.setCipher(sha1.getName());
    //set the next hash type to md5
    info.setHashType(md5.getName());
    
    
   
    em.addToDictionary(storedUser, DICTIONARY_CREDENTIALS, "password", info);
    
  
    
    //verify authorization works
    User authedUser = managementService.verifyAppUserPasswordCredentials(appId, username, password);

    assertEquals(userId, authedUser.getUuid());
    
    //test we can change the password
    String newPassword = "test2";
    
    managementService.setAppUserPassword(appId, userId, password, newPassword);
    
    //verify authorization works
    authedUser = managementService.verifyAppUserPasswordCredentials(appId, username, newPassword);

    assertEquals(userId, authedUser.getUuid());
  }
}
