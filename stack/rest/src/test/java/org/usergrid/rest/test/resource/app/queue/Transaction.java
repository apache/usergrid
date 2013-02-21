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
package org.usergrid.rest.test.resource.app.queue;

import java.util.List;
import java.util.Map;

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
public class Transaction extends CollectionResource {

  private String clientId;
  private long timeout = 0;

  /**
   * 
   */
  public Transaction(String transactionName, NamedResource parent) {
    super(transactionName, parent);
  }

  /**
   * Set the client id with the string
   * 
   * @param clientId
   * @return
   */
  public Transaction withClientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  /**
   * post to the entity set
   * 
   * @param entity
   * @return
   */
  public JsonNode delete() {
    return jsonMedia(withParams(withToken(resource()))).delete(JsonNode.class);
  }

  /**
   * Renew this transaction to the set timeout
   * 
   * @param timeout
   * @return
   */
  public JsonNode renew(long timeout) {
    this.timeout = timeout;
    return super.putInternal(null);
  }

  /**
   * Set the queue client ID if set
   * 
   * @param resource
   * @return
   */
  protected WebResource withParams(WebResource resource) {
    if (clientId != null) {
      resource = resource.queryParam("consumer", clientId);
    }
    if (timeout > 0) {
      resource = resource.queryParam("timeout", String.valueOf(timeout));
    }

    return resource;
  }

}
