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

import org.codehaus.jackson.JsonNode;
import org.usergrid.rest.test.resource.CollectionResource;
import org.usergrid.rest.test.resource.NamedResource;

/**
 * @author tnine
 * 
 */
public class SubscribersCollection extends CollectionResource {

  private String queueName;

  public SubscribersCollection(NamedResource parent) {
    super("subscribers", parent);
  }

  public JsonNode subscribe(String queueName) {
    this.queueName = queueName;
    return jsonMedia(withToken(resource())).put(JsonNode.class);

  }

  public JsonNode unsubscribe(String queueName) {
    this.queueName = queueName;
    return jsonMedia(withToken(resource())).delete(JsonNode.class);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.rest.test.resource.ValueResource#addToUrl(java.lang.StringBuilder
   * )
   */
  @Override
  public void addToUrl(StringBuilder buffer) {
    super.addToUrl(buffer);
    buffer.append(SLASH).append(queueName);
  }

}
