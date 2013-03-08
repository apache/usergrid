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

import java.util.UUID;

/**
 * @author tnine
 *
 */
public abstract class SetResource extends ValueResource {

  public SetResource(String name, NamedResource parent) {
    super(name, parent);
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




}
