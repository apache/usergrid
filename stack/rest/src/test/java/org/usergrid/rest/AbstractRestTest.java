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
package org.usergrid.rest;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.usergrid.utils.JsonUtils.mapToFormattedJsonString;
import static org.usergrid.utils.MapUtils.hashMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import me.prettyprint.hector.testutils.EmbeddedServerHelper;

import org.codehaus.jackson.JsonNode;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.usergrid.java.client.Client;
import org.usergrid.management.ManagementService;
import org.usergrid.persistence.EntityManagerFactory;
import org.xml.sax.SAXException;

import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import com.sun.jersey.test.framework.spi.container.TestContainerException;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;

/**
 * Base class for testing Usergrid Jersey-based REST API. Implementations should
 * model the paths mapped, not the method names. For example, to test the the
 * "password" mapping on applications.users.UserResource for a PUT method, the
 * test method(s) should following the following naming convention: test_[HTTP
 * verb]_[action mapping]_[ok|fail][_[specific failure condition if multiple]
 */
// @Autowire
public abstract class AbstractRestTest extends JerseyTest {

  /**
   * 
   */
  private static final int JETTY_PORT = 9998;

  private static final String CONTEXT = "/";

  private static Logger logger = LoggerFactory.getLogger(AbstractRestTest.class);

  static EmbeddedServerHelper embedded = null;
  static boolean usersSetup = false;
  protected static Properties properties;

  protected static String access_token;

  protected static String adminAccessToken;

  protected ManagementService managementService;

  static ClientConfig clientConfig = new DefaultClientConfig();

  protected static Client client;

  protected static final AppDescriptor descriptor;
  
  protected static ApplicationContext appCtx;

  static {
    clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);

    descriptor = new WebAppDescriptor.Builder("org.usergrid.rest").clientConfig(clientConfig).build();

    dumpClasspath(AbstractRestTest.class.getClassLoader());
    
  }

  public static void main(String... args) {
  }

  public static void dumpClasspath(ClassLoader loader) {
    System.out.println("Classloader " + loader + ":");

    if (loader instanceof URLClassLoader) {
      URLClassLoader ucl = (URLClassLoader) loader;
      System.out.println("\t" + Arrays.toString(ucl.getURLs()));
    } else {
      System.out.println("\t(cannot display components as not a URLClassLoader)");
    }

    if (loader.getParent() != null) {
      dumpClasspath(loader.getParent());
    }
  }

  public AbstractRestTest() throws TestContainerException {
    super(descriptor);
    setupUsers();
  }

  protected void setupUsers() {

    if (usersSetup) {
      return;
    }

    //
    createUser("edanuff", "ed@anuff.com", "sesame", "Ed Anuff"); // client.setApiUrl(apiUrl);

    usersSetup = true;

  }

  public void loginClient() throws InterruptedException {
    // now create a client that logs in ed

    // TODO T.N. This is a filthy hack and I should be ashamed of it (which
    // I am). There's a bug in the grizzly server when it's restarted per
    // test, and until we can upgrade versions this is the workaround. Backs
    // off with each attempt to allow the server to catch up
    for (int i = 0; i < 10; i++) {

      try {

        setUserPassword("ed@anuff.com", "sesame");

        client = new Client("test-organization", "test-app").withApiUrl(getBaseURI().toString());

        org.usergrid.java.client.response.ApiResponse response = client.authorizeAppUser("ed@anuff.com", "sesame");

        assertTrue(response != null && response.getError() == null);

        break;
      } catch (ResourceAccessException rae) {
        // swallow and try again. Bug in grizzly server causes a socket
        // exception occasionally
        logger.error("Ignoring exception and retrying", rae);
        Thread.sleep(100 * i);
      }

    }

  }

  @Override
  protected TestContainerFactory getTestContainerFactory() {
    // return new
    // com.sun.jersey.test.framework.spi.container.grizzly2.web.GrizzlyWebTestContainerFactory();
    return new com.sun.jersey.test.framework.spi.container.external.ExternalTestContainerFactory();
  }

  @BeforeClass
  public static void setup() throws Exception {
    // Class.forName("jsp.WEB_002dINF.jsp.org.usergrid.rest.TestResource.error_jsp");
    logger.info("setup");
    assertNull(embedded);
    embedded = new EmbeddedServerHelper();
    embedded.setup();

    appCtx = new ClassPathXmlApplicationContext("classpath:/usergrid-rest-context-test.xml");
    
    startJetty();

  }

  private static void startJetty() throws Exception {
    Server server = new Server(JETTY_PORT);
    server.setHandler(new WebAppContext("src/main/webapp", CONTEXT));
    server.start();

  }

  @AfterClass
  public static void teardown() throws Exception {
    logger.info("teardown");
    EmbeddedServerHelper.teardown();
    embedded = null;
  }

  public static void logNode(JsonNode node) {
    logger.info(mapToFormattedJsonString(node));
  }

  /**
   * Hook to get the token for our base user
   */
  @Before
  public void acquireToken() throws Exception {

    
    properties = (Properties) appCtx.getBean("properties");

    managementService = (ManagementService) appCtx.getBean("managementService");

    access_token = userToken("ed@anuff.com", "sesame");

    loginClient();

  }

  protected String userToken(String name, String password) throws Exception {

    setUserPassword("ed@anuff.com", "sesame");

    JsonNode node = resource().path("/test-organization/test-app/token").queryParam("grant_type", "password")
        .queryParam("username", name).queryParam("password", password).accept(MediaType.APPLICATION_JSON)
        .get(JsonNode.class);

    String userToken = node.get("access_token").getTextValue();

    return userToken;

  }

  public void createUser(String username, String email, String password, String name) {

    if (adminAccessToken == null) {
      adminToken();
    }

    Map<String, String> payload = hashMap("email", email).map("username", username).map("name", name)
        .map("password", password).map("pin", "1234");

    resource().path("/test-organization/test-app/users").queryParam("access_token", adminAccessToken)
        .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, payload);

  }

  public void setUserPassword(String username, String password) {
    Map<String, String> data = new HashMap<String, String>();
    data.put("newpassword", password);

    if (adminAccessToken == null) {
      adminToken();
    }

    // change the password as admin. The old password isn't required
    JsonNode node = resource().path(String.format("/test-organization/test-app/users/%s/password", username))
        .queryParam("access_token", adminAccessToken).accept(MediaType.APPLICATION_JSON)
        .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, data);

    assertNull(getError(node));

  }

  /**
   * Acquire the management token for the test@usergrid.com user with the
   * default password
   * 
   * @return
   */
  protected String adminToken() {
    adminAccessToken = mgmtToken("test@usergrid.com", "test");
    return adminAccessToken;
  }

  /**
   * Get the super user's access token
   * 
   * @return
   */
  protected String superAdminToken() {
    return mgmtToken("superuser", "superpassword");
  }

  /**
   * Acquire the management token for the test@usergrid.com user with the given
   * password
   * 
   * @return
   */
  protected String mgmtToken(String user, String password) {
    JsonNode node = resource().path("/management/token").queryParam("grant_type", "password")
        .queryParam("username", user).queryParam("password", password).accept(MediaType.APPLICATION_JSON)
        .get(JsonNode.class);

    String mgmToken = node.get("access_token").getTextValue();

    return mgmToken;

  }

  /**
   * Get the entity from the entity array in the response
   * 
   * @param response
   * @param index
   * @return
   */
  protected JsonNode getEntity(JsonNode response, int index) {
    return response.get("entities").get(index);
  }

  /**
   * Get the entity from the entity array in the response
   * 
   * @param response
   * @param index
   * @return
   */
  protected JsonNode getEntity(JsonNode response, String name) {
    return response.get("entities").get(name);
  }

  /**
   * Get the uuid from the entity at the specified index
   * 
   * @param response
   * @param index
   * @return
   */
  protected UUID getEntityId(JsonNode response, int index) {
    return UUID.fromString(getEntity(response, index).get("uuid").asText());
  }

  /**
   * Get the error response
   * 
   * @param response
   * @return
   */
  protected JsonNode getError(JsonNode response) {
    return response.get("error");
  }
}
