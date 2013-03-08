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

import java.util.UUID;

import org.usergrid.rest.test.resource.EntityResource;
import org.usergrid.rest.test.resource.NamedResource;


/**
 * A resource for testing queues
 * 
 * @author tnine
 *
 */
public class User extends EntityResource {

  /**
   * @param entityId
   * @param parent
   */
  public User(UUID entityId, NamedResource parent) {
    super(entityId, parent);
  }

  /**
   * @param entityName
   * @param parent
   */
  public User(String entityName, NamedResource parent) {
    super(entityName, parent);
  }

  
  
  

  
  
  
}
