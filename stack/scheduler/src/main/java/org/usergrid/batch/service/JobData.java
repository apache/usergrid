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
package org.usergrid.batch.service;

import org.usergrid.persistence.TypedEntity;
import org.usergrid.persistence.annotations.EntityProperty;

/**
 * All job data should be contained in this entity
 * 
 * @author tnine
 * 
 */
public class JobData extends TypedEntity {

  private static final String NAME = "jobdata";

  @EntityProperty(required = true, basic = true, indexed = true)
  private String jobName;

  @EntityProperty(required = true, basic = true, indexed = true)
  private long fireTime;

  // this is ugly, but the only way to register this class once with the schema
  // manager
//  static {
//    Schema.getDefaultSchema().registerEntity(JobData.class);
//  }

  /**
   * @param jobName
   * @param fireTime
   */
  public JobData() {
    super();
  }

  /**
   * @param jobName
   * @param fireTime
   */
  public JobData(String jobName, long fireTime) {
    super();
    this.jobName = jobName;
    this.fireTime = fireTime;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.usergrid.persistence.AbstractEntity#getType()
   */
  @Override
  @EntityProperty(required = true, mutable = false, basic = true, indexed = false)
  public String getType() {
    return NAME;
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

  /**
   * @return the fireTime
   */
  public long getFireTime() {
    return fireTime;
  }

  /**
   * @param fireTime
   *          the fireTime to set
   */
  public void setFireTime(long fireTime) {
    this.fireTime = fireTime;
  }

  // /**
  // *
  // */
  // public JobData() {
  // }
  //
  // /* (non-Javadoc)
  // * @see org.usergrid.persistence.DynamicEntity#getType()
  // */
  // @Override
  // @EntityProperty(required = true, mutable = false, basic = true, indexed =
  // false)
  // public String getType() {
  // return NAME;
  // }
  //
  // /* (non-Javadoc)
  // * @see org.usergrid.persistence.DynamicEntity#setType(java.lang.String)
  // */
  // @Override
  // public void setType(String type) {
  // //do nothing, no op on purpose
  // }
  //

}
