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

import static org.junit.Assert.assertEquals;

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

  public void addToUrlEnd(StringBuilder buffer) {
    buffer.append(SLASH);
    buffer.append(buffer);
  }

  /**
   * Create a new entity with the specified data
   * @param entity
   * @return
   */
  public JsonNode create(Map<String, ? > entity){
    return postInternal(entity);
  }


  public void delete (Map<String,?> entity) {
    deleteInternal();
  }

  protected void deleteInternal() {
    withParams(withToken(resource()))
        .delete(JsonNode.class);
    //json.delete(JsonNode.class);
  }
 // public String delete(@PathParam("entity"))
  
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

  public JsonNode put (Map<String,?> entity) {

    return putInternal(entity);
  }

  /**
   * put to the entity set
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
   * Query this resource.
   */
  public JsonNode query(String query) {
    return getInternal(query, null);
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

  /*created so a limit could be easily added. Consider merging with getInternal(query,cursor)
  as those are the only two query input parameters.
   */
  public JsonNode query(String query,String addition,String numAddition){
    return getInternal(query,addition,numAddition);
  }

  protected JsonNode getInternal(String query,String addition, String numAddition)
  {
    WebResource resource = withParams(withToken(resource())).queryParam("ql", query);

    if (addition != null) {
      resource = resource.queryParam(addition, numAddition);
    }

    return jsonMedia(resource).get(JsonNode.class);
  }


  /*take out JsonNodes and call quereies inside the function*/
  /*call limit and just loop through that instead of having to do cursors and work
  making it more complicated. */
  /* Test will only handle values under 1000 really due to limit size being that big */
  public int verificationOfQueryResults(String query,String checkedQuery) {

    int totalEntitiesContained = 0;
    JsonNode correctNode = this.query(query,"limit","1000");
    JsonNode checkedNodes = this.query(checkedQuery,"limit","1000");

    while (correctNode.get("entities") != null)
    {
      totalEntitiesContained += correctNode.get("entities").size();

      for(int index = 0; index < correctNode.get("entities").size();index++)
        assertEquals(correctNode.get("entities").get(index),checkedNodes.get("entities").get(index));

      if(checkedNodes.get("cursor") != null || correctNode.get("cursor") != null) {
        checkedNodes = this.query(checkedQuery,"cursor",checkedNodes.get("cursor").toString());
        correctNode = this.query(query,"cursor",correctNode.get("cursor").toString());
      }

//      if(correctNode.get("cursor") != null)
//        correctNode = this.query(query,"cursor",correctNode.get("cursor").toString());
      else
        break;
    }
    return totalEntitiesContained;
  }
  public int countEntities (String query) {

    int totalEntitiesContained =0;
    JsonNode correctNode = this.query(query);

    while (correctNode.get("entities") != null) {
      totalEntitiesContained += correctNode.get("entities").size();
      if(correctNode.get("cursor") != null)
        correctNode = this.query(query,"cursor",correctNode.get("cursor").toString());
    }
    return totalEntitiesContained;
  }

  /*cut out the key variable argument and move it into the customcollection call
  then just have it automatically add in the variable. */

  public void createEntitiesWithOrdinal(Map valueHolder,int numOfValues) {

    for(int i = 0; i < numOfValues; i++) {
      valueHolder.put("Ordinal",i);
      this.create(valueHolder);
    }
  }

}
