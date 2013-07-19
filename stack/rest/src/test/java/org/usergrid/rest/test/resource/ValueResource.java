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
import java.util.UUID;

import org.codehaus.jackson.JsonNode;

import com.sun.jersey.api.client.WebResource;

/**
 * @author tnine
 * 
 */
public abstract class ValueResource extends NamedResource {

  private String name;
  private String query;
  private String cursor;
  private UUID start;

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
   * Create a new entity with the specified data
   * @param entity
   * @return
   */
  public JsonNode create(Map<String, ? > entity){
    return postInternal(entity);
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
   * Get the data
   * @return
   */
  public JsonNode get(){
    return getInternal();
  }


  @SuppressWarnings("unchecked")
  public <T extends ValueResource> T withCursor(String cursor){
    this.cursor = cursor;
    return (T) this;
  }
  
  
  @SuppressWarnings("unchecked")
  public <T extends ValueResource> T withQuery(String query){
    this.query = query;
    return (T) this;
  }
  
  @SuppressWarnings("unchecked")
  public <T extends ValueResource> T withStart(UUID start){
    this.start = start;
    return (T) this;
  }
  
  
  
  /**
   * Get entities in this collection. Cursor is optional
   * 
   * @param query
   * @param cursor
   * @return
   */
  protected JsonNode getInternal() {

    
    WebResource resource = withParams(withToken(resource()));
    
    if(query != null){
      resource = resource.queryParam("ql", query);
    }

    if (cursor != null) {
      resource = resource.queryParam("cursor", cursor);
    }
    
    if(start != null){
      resource = resource.queryParam("start", start.toString());
    }

    return jsonMedia(resource).get(JsonNode.class);
  }
  

  /**
   * Get entities in this collection. Cursor is optional
   * 
   * @param query
   * @param cursor
   * @return
   */
  protected JsonNode deleteInternal() {

    
    WebResource resource = withParams(withToken(resource()));
    
    if(query != null){
      resource = resource.queryParam("ql", query);
    }

    if (cursor != null) {
      resource = resource.queryParam("cursor", cursor);
    }
    
    if(start != null){
      resource = resource.queryParam("start", start.toString());
    }

    return jsonMedia(resource).delete(JsonNode.class);
  }
  

}
