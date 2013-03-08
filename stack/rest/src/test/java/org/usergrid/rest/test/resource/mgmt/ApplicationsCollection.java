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

import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.usergrid.rest.test.resource.CollectionResource;
import org.usergrid.rest.test.resource.NamedResource;
import org.usergrid.utils.MapUtils;

/**
 * @author tnine
 *
 */
public class ApplicationsCollection extends CollectionResource {

  /**
   * @param collectionName
   * @param parent
   */
  public ApplicationsCollection(NamedResource parent) {
    super("apps", parent);
  }
  
  public Application application(String name){
    return new Application(name, this);
  }
  
  /**
   * Create the org and return it's UUID
   * @param name
   * @param owner
   * @return
   */
  public UUID create(String name){
    
    JsonNode node =  postInternal(MapUtils.hashMap("name", name));
    
    return getEntityId(node, 0);
    
    
  }

  
}
