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
package org.usergrid.persistence.query.ir.result;

import java.util.List;
import java.util.UUID;

import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.Results;

/**
 * @author tnine
 *
 */
public class ConnectionRefLoader implements ResultsLoader {

  private EntityManager em;
  
  /**
   * 
   */
  public ConnectionRefLoader(EntityManager em) {
    this.em = em;
  }

  /* (non-Javadoc)
   * @see org.usergrid.persistence.query.ir.result.ResultsLoader#getResults(java.util.List)
   */
  @Override
  public Results getResults(List<UUID> entityIds) throws Exception {
   return null;
  }

}
