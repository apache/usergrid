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
package org.usergrid.rest.test.resource.user;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;
import org.usergrid.rest.test.resource.CollectionResource;
import org.usergrid.rest.test.resource.NamedResource;

import com.sun.jersey.api.client.WebResource;

/**
 * A resource for testing queues
 * 
 * @author tnine
 * 
 */
public class Queue extends CollectionResource {

  private String clientId;

  /**
   * 
   */
  public Queue(String queueName, NamedResource parent) {
    super(queueName, parent);
  }

  public Queue withClientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  public Object post(Map<String, ?> payload) {
    JsonNode node = super.postInternal(payload);
    return node;
  }

  /**
   * Get entities in this collection. Cursor is optional
   * 
   * @param query
   * @param cursor
   * @return
   */
  public JsonNode get() {
    return jsonMedia(withClientId(withToken(resource()))).get(JsonNode.class);
  }

  /**
   * Set the queue client ID if set
   * 
   * @param resource
   * @return
   */
  private WebResource withClientId(WebResource resource) {
    if (clientId == null) {
      return resource;
    }

    return resource.queryParam("consumer", clientId);
  }

  /**
   * Get the next entry in the queue.  Returns null if one doesn't exist
   * 
   * @return
   */
  public JsonNode getNextEntry() {
    List<JsonNode> messages = getNodesAsList("messages", get());

    return messages.size() == 1 ? messages.get(0) : null;
  }

}
