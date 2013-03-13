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

/**
 * @author tnine
 *
 */
public class EntityResource extends NamedResource {

  private String entityName;
  private UUID entityId;
  
  
  public EntityResource(String entityName, NamedResource parent){
    super(parent);
    this.entityName = entityName;
  }
  
  public EntityResource(UUID entityId, NamedResource parent){
    super(parent);
    this.entityId = entityId;
  }


  /**
   * Get the connection
   * @param name
   * @return
   */
  public ConnectionResource connetion(String name){
    return new ConnectionResource(name, this);
  }


  /* (non-Javadoc)
   * @see org.usergrid.rest.resource.NamedResource#addToUrl(java.lang.StringBuffer)
   */
  @Override
  public void addToUrl(StringBuilder buffer) {
    parent.addToUrl(buffer);
    
    buffer.append(SLASH);
    
    if(entityId != null){
      buffer.append(entityId.toString());  
    }else if (entityName != null){
      buffer.append(entityName);
    }
     
  }
  
  
  
  /**
   * post to the entity set
   * @param entity
   * @return
   */
  protected JsonNode post(Map<String, Object> entity){
    return jsonMedia(withToken(resource())).post(JsonNode.class, entity);
  }
  
  /**
   * post to the entity set
   * @param entity
   * @return
   */
  protected JsonNode put(Map<String, Object> entity){
    return jsonMedia(withToken(resource())).put(JsonNode.class, entity);
  }
  
  
  /**
   * post to the entity set
   * @param entity
   * @return
   */
  protected JsonNode delete(){
    return jsonMedia(withToken(resource())).delete(JsonNode.class);
  }
  
  /**
   * Get the resource
   * @return
   */
  protected JsonNode get(){
    return jsonMedia(withToken(resource())).get(JsonNode.class);
  }
 
  
  
}
