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
package org.usergrid.rest.test.resource.app;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;
import org.usergrid.mq.QueuePosition;
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
  private int next = 0;
  private int previous = 0;
  private String position;
  private String last;

  /**
   * 
   */
  public Queue(String queueName, NamedResource parent) {
    super(queueName, parent);
  }

  /**
   * Set the client id with the string
   * 
   * @param clientId
   * @return
   */
  public Queue withClientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  /**
   * Set this with the next page size
   * 
   * @param nextSize
   * @return
   */
  public Queue withNext(int nextSize) {
    this.next = nextSize;
    return this;
  }
  
  /**
   * Set this with the next page size
   * 
   * @param nextSize
   * @return
   */
  public Queue withPrevious(int previous) {
    this.previous = previous;
    return this;
  }
  
  public Queue withPosition(String position){
    this.position = position;
    return this;
  }
  
  public Queue withLast(String last){
    this.last = last;
    return this;
  }

  /**
   * @return
   */
  public SubscribersCollection subscribers() {
    return new SubscribersCollection(this);
  }

  public JsonNode post(Map<String, ?> payload) {
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
    return jsonMedia(withQueueParams(withToken(resource()))).get(JsonNode.class);
  }

  /**
   * post to the entity set
   * 
   * @param entity
   * @return
   */
  public JsonNode delete() {
    return jsonMedia(withToken(resource())).delete(JsonNode.class);
  }

  /**
   * Set the queue client ID if set
   * 
   * @param resource
   * @return
   */
  private WebResource withQueueParams(WebResource resource) {
    if (clientId != null) {
      resource = resource.queryParam("consumer", clientId);
    }
    if(position != null){
      resource = resource.queryParam("pos", position);
    }
    if(last != null){
      resource = resource.queryParam("last", last);
    }
    
    if (next > 0) {
      resource = resource.queryParam("next", String.valueOf(next));
    }
    
    if (previous > 0) {
      resource = resource.queryParam("prev", String.valueOf(next));
    }

    return resource;
  }

  /**
   * Get the next entry in the queue. Returns null if one doesn't exist
   * 
   * @return
   */
  public JsonNode getNextEntry() {
    List<JsonNode> messages = getNodesAsList("messages", get());

    return messages.size() == 1 ? messages.get(0) : null;
  }
  
  /**
   * Get the json response of the messages nodes
   * @return
   */
  public List<JsonNode> getNextPage(){
    JsonNode response = get();
    
    JsonNode last = response.get("last");
    
    if(last != null){
      this.last = last.asText();
    }
    
    return getNodesAsList("messages", response);
  }

}
