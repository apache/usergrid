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

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;
import org.usergrid.rest.test.security.TestUser;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;

/**
 * @author tnine
 *
 */
public abstract class NamedResource {
  
  protected static final String SLASH = "/";
  
  protected NamedResource parent;
  
  /**
   * 
   */
  public NamedResource(NamedResource parent) {
    this.parent = parent;
  }
  
  /**
   * Get the url to this resource 
   **/
  public String url() {
    StringBuilder buff = new StringBuilder();
    addToUrl(buff);
    return buff.toString();
  }
  
  /**
   * Get the resouce for calling the url
   * @return
   */
  protected WebResource resource(){
    return parent.resource();
  }
  
  protected String token(){
    return parent.token();
  }
  
  

  /**
   * Add itself to the end of the URL. Should not append a final "/"
   * @param buffer
   */
  public abstract void addToUrl(StringBuilder buffer);
}
