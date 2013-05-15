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
package org.usergrid.rest.applications.users;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNull;

import org.codehaus.jackson.JsonNode;
import org.junit.Ignore;
import org.junit.Test;
import org.usergrid.rest.RestContextTest;
import org.usergrid.rest.test.resource.app.Device;
import org.usergrid.rest.test.resource.app.queue.DevicesCollection;
import org.usergrid.rest.test.security.TestAppUser;
import org.usergrid.rest.test.security.TestUser;
import org.usergrid.utils.MapUtils;

/**
 * 
 */
public class UserOwnershipResourceTest extends RestContextTest {

  @Test
  public void usernameQuery() {
    
   
    //anonymous user
    context.clearUser();
    
    TestUser user1 = new TestAppUser("testuser1@usergrid.org", "password",  "testuser1@usergrid.org").create(context).login(context).makeActive(context);

    // create device 1 on user1 devices
    context.application().users().user("me").devices().create(MapUtils.hashMap("name", "device1").map("number", "5551112222"));

    //anonymous user
    context.clearUser();
    
    // create device 2 on user 2
    TestUser user2 = new TestAppUser("testuser2@usergrid.org", "password","testuser2@usergrid.org").create(context).login(context).makeActive(context);

    context.application().users().user("me").devices().create(MapUtils.hashMap("name", "device2").map("number", "5552223333"));

    // now query on user 1.

    context.withUser(user1);
    
    JsonNode data = context.application().users().user("me").devices().device("device1").get();
    assertNotNull(data);
    assertEquals("device1", getEntity(data, 0).get("name").asText());

    // check we can't see device2
    data = context.application().users().user("me").devices().device("device2").get();
    assertNull(getEntity(data, 0));

    // log in as user 2 and check it
    context.withUser(user2);
    
    data = context.application().users().user("me").devices().device("device2").get();
    assertNotNull(data);
    assertEquals("device2", getEntity(data, 0).get("name").asText());

    // check we can't see device1
    data = context.application().users().user("me").devices().device("device1").get();
    assertNull(getEntity(data, 0));

    // we can see both of these
    DevicesCollection devices = context.application().devices();

    //test for user 1
    user1.login(context);
    data = devices.device("device1").get();

    assertNotNull(data);
    assertEquals("device1", getEntity(data, 0).get("name").asText());

    data = devices.device("device2").get();

    assertNotNull(data);
    assertEquals("device2", getEntity(data, 0).get("name").asText());

    data = devices.device("device2").get();

    assertNull(data);

    //test for user 2
    user2.login(context);
    data = devices.device("device1").get();

    assertNotNull(data);
    assertEquals("device1", getEntity(data, 0).get("name").asText());

    data = devices.device("device2").get();

    assertNotNull(data);
    assertEquals("device2", getEntity(data, 0).get("name").asText());

    data = devices.device("device2").get();

    assertNull(data);
  }
  
  
  

}
