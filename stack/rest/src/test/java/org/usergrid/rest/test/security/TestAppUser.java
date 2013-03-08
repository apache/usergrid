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
package org.usergrid.rest.test.security;

import org.usergrid.rest.test.resource.TestContext;


/**
 * @author tnine
 *
 */
public  class TestAppUser extends TestUser{

  /**
   * @param user
   * @param password
   * @param email
   */
  public TestAppUser(String user, String password, String email) {
    super(user, password, email);
  }

  /* (non-Javadoc)
   * @see org.usergrid.rest.test.security.TestUser#getToken(java.lang.String, java.lang.String, org.usergrid.rest.test.resource.TestContext)
   */
  @Override
  protected String getToken(String username, String password, TestContext context) {
    return context.application().token(username, password);
  }

}
