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
package org.usergrid.rest.test.resource.mgmt;

import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;
import org.usergrid.rest.test.resource.Me;
import org.usergrid.rest.test.resource.NamedResource;
import org.usergrid.rest.test.resource.RootResource;
import org.usergrid.rest.test.resource.app.UsersCollection;
import org.usergrid.utils.MapUtils;

/**
 * @author tnine
 * 
 */
public class Management extends NamedResource {

  /**
   * @param parent
   */
  public Management(RootResource root) {
    super(root);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.rest.resource.NamedResource#addToUrl(java.lang.StringBuilder)
   */
  @Override
  public void addToUrl(StringBuilder buffer) {
    parent.addToUrl(buffer);
    buffer.append(SLASH);
    buffer.append("management");
  }

  public UsersCollection users() {
    return new UsersCollection(this);
  }

  public OrganizationsCollection orgs() {
    return new OrganizationsCollection(this);
  }

  /**
   * Get the token from management for this username and password
   * 
   * @param username
   * @param password
   * @return
   */
  public String tokenGet(String username, String password) {

    JsonNode node = resource().path(String.format("%s/token", url())).queryParam("grant_type", "password")
        .queryParam("username", username).queryParam("password", password).accept(MediaType.APPLICATION_JSON)
        .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

    return node.get("access_token").asText();
  }

  /**
   * Get the token from management for this username and password
   * 
   * @param username
   * @param password
   * @return
   */
  public String tokenPost(String username, String password) {

    Map<String, String> payload = MapUtils.hashMap("grant_type", "password").map("username", username)
        .map("password", password);

    JsonNode node = resource().path(String.format("%s/token", url())).accept(MediaType.APPLICATION_JSON)
        .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, payload);

    return node.get("access_token").asText();
  }

 
  public Me me(){
    return new Me(this);
  }

}
