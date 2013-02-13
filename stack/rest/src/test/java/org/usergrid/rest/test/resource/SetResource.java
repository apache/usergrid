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

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;

import com.sun.jersey.api.client.WebResource;

/**
 * @author tnine
 *
 */
public abstract class SetResource extends NamedResource {

  private String name;

  public SetResource(String name, NamedResource parent) {
    super(parent);
    this.name = name;
  }



  /* (non-Javadoc)
   * @see org.usergrid.rest.resource.NamedResource#addToUrl(java.lang.StringBuilder)
   */
  @Override
  public void addToUrl(StringBuilder buffer) {
    parent.addToUrl(buffer);
    
    buffer.append(SLASH);
    
    buffer.append(name);
  }
  

  
  
  /**
   * Get an entity resource by name
   * @param name
   * @return
   */
  public EntityResource entity(String name){
    return new EntityResource(name, this); 
  }
  
  /**
   * Get an entity resource by Id
   * @param id
   * @return
   */
  public EntityResource entity(UUID id){
    return new EntityResource(id, this);
  }


  /**
   * post to the entity set
   * @param entity
   * @return
   */
  public JsonNode post(Map<String, ?> entity){
    return resource().path(url()).queryParam("access_token",token())
        .accept(MediaType.APPLICATION_JSON)
        .type(MediaType.APPLICATION_JSON_TYPE).post(JsonNode.class, entity);
  }
  
  /**
   * Get a list of entities
   * @return
   */
  public JsonNode get(){
    return resource().path(url()).queryParam("access_token",token())
        .accept(MediaType.APPLICATION_JSON)
        .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);
  }
  
  /**
   * Get entities in this collection.  Cursor is optional
   * @param query
   * @param cursor
   * @return
   */
  public JsonNode get(String query, String cursor){
    
     WebResource resource =  resource().path(url()).queryParam("access_token",token()).queryParam("ql",query);
     
     if(cursor != null){
       resource = resource.queryParam("cursor", cursor);
     }
        
     return resource.accept(MediaType.APPLICATION_JSON)
        .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);
  }
  


}
