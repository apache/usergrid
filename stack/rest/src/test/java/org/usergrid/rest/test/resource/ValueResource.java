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

import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;

import com.sun.jersey.api.client.WebResource;

/**
 * @author tnine
 * 
 */
public abstract class ValueResource extends NamedResource {

  private String name;

  public ValueResource(String name, NamedResource parent) {
    super(parent);
    this.name = name;
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

    buffer.append(name);
  }

  /**
   * post to the entity set
   * 
   * @param entity
   * @return
   */
  protected JsonNode postInternal(Map<String, ?> entity) {
   
    return jsonMedia(withParams(withToken(resource())))
        .post(JsonNode.class, entity);
  }
  
  /**
   * post to the entity set
   * 
   * @param entity
   * @return
   */
  protected JsonNode postInternal(Map<String, ?>[] entity) {
   
    return jsonMedia(withParams(withToken(resource())))
        .post(JsonNode.class, entity);
  }
  
  /**
   * post to the entity set
   * 
   * @param entity
   * @return
   */
  protected JsonNode putInternal(Map<String, ?> entity) {
   
    return jsonMedia(withParams(withToken(resource())))
        .put(JsonNode.class, entity);
  }

  /**
   * Get a list of entities
   * 
   * @return
   */
  protected JsonNode getInternal() {
    return jsonMedia(withParams(withToken(resource()))).get(JsonNode.class);
  }

  /**
   * Get entities in this collection. Cursor is optional
   * 
   * @param query
   * @param cursor
   * @return
   */
  protected JsonNode getInternal(String query, String cursor) {

    
    WebResource resource = withParams(withToken(resource())).queryParam("ql", query);

    if (cursor != null) {
      resource = resource.queryParam("cursor", cursor);
    }

    return jsonMedia(resource).get(JsonNode.class);
  }

}
