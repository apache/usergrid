/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file 
 * except  in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 ******************************************************************************/
package org.usergrid.rest.applications.collection.groups;

import com.sun.jersey.api.client.UniformInterfaceException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.rest.AbstractRestIT;
import org.usergrid.rest.TestContextSetup;

public class UpdateGroupIT extends AbstractRestIT {
  private static final Logger logger = LoggerFactory.getLogger(UpdateGroupIT.class);

  @Rule
  public TestContextSetup context = new TestContextSetup(this);

  @Test // USERGRID-1729
  public void updateGroupWithSameNameAsApp() throws IOException {

    // create groupMap with same name as app
    String groupId = null;
    String groupPath = context.getAppName();
    try {
      Map<String, Object> groupMap = new HashMap<String, Object>();
      groupMap.put("title", "Old Title");
      groupMap.put("path", groupPath);
      JsonNode groupJson = resource().path(context.getOrgName() + "/" + context.getAppName() + "/groups")
        .queryParam("access_token", context.getActiveUser().getToken())
        .accept(MediaType.APPLICATION_JSON)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .post(JsonNode.class, groupMap);
      groupId = groupJson.get("entities").get(0).get("uuid").getTextValue();

    } catch (UniformInterfaceException e) {
      fail("Error creating group: " + IOUtils.toString(e.getResponse().getEntityInputStream()));
    }

    assertTitle(groupId, "Old Title");

    // update that group by giving it a new title and using group path in URL
    try {
      Map<String, Object> group = new HashMap<String, Object>();
      group.put("title", "New Title");
      resource().path(context.getOrgName() + "/" + context.getAppName() + "/groups/" + groupPath)
        .queryParam("access_token", context.getActiveUser().getToken())
        .accept(MediaType.APPLICATION_JSON)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .put(JsonNode.class, group);

    } catch (UniformInterfaceException e) {
      fail("Error updating group: " + IOUtils.toString(e.getResponse().getEntityInputStream()));
    }

    assertTitle(groupId, "New Title");

    // update that group by giving it a new title and using UUID in URL
    try {
      Map<String, Object> group = new HashMap<String, Object>();
      group.put("title", "Even Newer Title");
      resource().path(context.getOrgName() + "/" + context.getAppName() + "/groups/" + groupId)
        .queryParam("access_token", context.getActiveUser().getToken())
        .accept(MediaType.APPLICATION_JSON)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .put(JsonNode.class, group);

    } catch (UniformInterfaceException e) {
      fail("Error updating group: " + IOUtils.toString(e.getResponse().getEntityInputStream()));
    }

    assertTitle(groupId, "Even Newer Title");
  }

  private void assertTitle(String groupId, String title) {
    JsonNode groupJson = resource().path(context.getOrgName() + "/" + context.getAppName() + "/groups/" + groupId)
      .queryParam("access_token", context.getActiveUser().getToken())
      .accept(MediaType.APPLICATION_JSON)
      .type(MediaType.APPLICATION_JSON_TYPE)
      .get(JsonNode.class);
    Assert.assertEquals(title, groupJson.get("entities").get(0).get("title").getTextValue());
  }
}
