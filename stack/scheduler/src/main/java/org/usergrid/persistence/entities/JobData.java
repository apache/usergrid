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
package org.usergrid.persistence.entities;

import org.usergrid.persistence.TypedEntity;
import org.usergrid.persistence.annotations.EntityProperty;

/**
 * All job data should be contained in this entity
 * 
 * @author tnine
 * 
 */
public class JobData extends TypedEntity {


  @EntityProperty(required = true, basic = true, indexed = true)
  private String jobName;

 

  /**
   * @param jobName
   * @param startTime
   */
  public JobData() {
    super();
  }


  /**
   * @return the jobName
   */
  public String getJobName() {
    return jobName;
  }

  /**
   * @param jobName
   *          the jobName to set
   */
  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  

}
