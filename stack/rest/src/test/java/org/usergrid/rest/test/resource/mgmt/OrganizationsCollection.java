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

import org.codehaus.jackson.JsonNode;
import org.usergrid.rest.test.resource.CollectionResource;
import org.usergrid.rest.test.resource.NamedResource;
import org.usergrid.rest.test.security.TestUser;
import org.usergrid.utils.MapUtils;

/**
 * @author tnine
 *
 */
public class OrganizationsCollection extends CollectionResource {

  /**
   * @param collectionName
   * @param parent
   */
  public OrganizationsCollection(NamedResource parent) {
    super("orgs", parent);
  }
  
  public Organization organization(String name){
    return new Organization(name, this);
  }
  
  public void create(String name, TestUser owner){
    
    JsonNode node =  post(MapUtils.hashMap("organization", name).map("username", owner.getUser()).map("email", owner.getEmail()).map("name", owner.getUser()).map("password", owner.getPassword()));
    
    
    
  }

}
