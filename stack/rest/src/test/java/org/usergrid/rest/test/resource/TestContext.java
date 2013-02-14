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
package org.usergrid.rest.test.resource;

import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.usergrid.rest.test.resource.mgmt.Management;
import org.usergrid.rest.test.resource.user.Application;
import org.usergrid.rest.test.resource.user.User;
import org.usergrid.rest.test.resource.user.UsersCollection;
import org.usergrid.rest.test.security.TestUser;

import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * @author tnine
 * 
 */
public class TestContext {

  private JerseyTest test;
  private TestUser activeUser;
  private String orgName;
  private UUID orgUuid;
  private String appName;
  private UUID appUuid;

  /**
   * 
   */
  private TestContext(JerseyTest test) {
    this.test = test;
  }

  /**
   * Create a test context
   * 
   * @param test
   * @return
   */
  public static TestContext create(JerseyTest test) {
    return new TestContext(test);
  }

  public TestContext withUser(TestUser user) {
    this.activeUser = user;
    return this;
  }

  public TestContext withOrg(String orgName) {
    this.orgName = orgName;
    return this;
  }

  public TestContext withApp(String appName) {
    this.appName = appName;
    return this;
  }


  /**
   * Creates the org specified
   * 
   * @return
   */
  public TestContext createNewOrgAndUser() {
    orgUuid = managment().orgs().create(orgName, activeUser);

    return this;
  }

  /**
   * Creates the org specified
   * 
   * @return
   */
  public TestContext createAppForOrg() {
    appUuid = managment().orgs().organization(orgName).apps().create(appName);

    return this;
  }

    /**
   * Create the app if it doesn't exist with the given TestUser. If the app
   * exists, the user is logged in
   * 
   * @return
   */
  public TestContext loginUser() {
    // nothing to do
    if (activeUser.isLoggedIn()) {
      return this;
    }

    // try to log in the user first
    activeUser.login(this);

    return this;

  }

  /**
   * Get the users resource for the application
   * 
   * @return
   */
  public UsersCollection users() {
    return application().users();
  }

  /**
   * Get the app user resource
   * 
   * @param username
   * @return
   */
  public User user(String username) {
    return application().users().user(username);
  }

  /**
   * @return the orgUuid
   */
  public UUID getOrgUuid() {
    return orgUuid;
  }

  /**
   * @return the appUuid
   */
  public UUID getAppUuid() {
    return appUuid;
  }

  /**
   * Get the application resource
   * 
   * @return
   */
  public Application application() {
    return new Application(orgName, appName, root());
  }

  public Management managment() {
    return new Management(root());
  }

  protected RootResource root() {
    return new RootResource(test.resource(), activeUser.getToken());
  }

  /**
   * Calls createNewOrgAndUser, logs in the user, then creates the app. All in 1
   * call.
   * 
   * @return
   */
  public TestContext initAll() {
    return createNewOrgAndUser().loginUser().createAppForOrg();
  }

}
