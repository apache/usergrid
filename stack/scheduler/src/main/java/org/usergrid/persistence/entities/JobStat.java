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

import java.util.UUID;

import org.usergrid.persistence.TypedEntity;
import org.usergrid.persistence.annotations.EntityProperty;

/**
 * All job execution info should be in this entity
 * 
 * @author tnine
 * 
 */
public class JobStat extends TypedEntity {


  @EntityProperty(required = true, basic = true, indexed = true)
  private String jobName;
  
  @EntityProperty(required = true, basic = true, indexed = true)
  private UUID jobId;

  @EntityProperty(required = true, basic = true, indexed = true)
  private long startTime;
  
  @EntityProperty(required = true, basic = true, indexed = true)
  private int runCount;
  
  @EntityProperty(required = true, basic = true, indexed = true)
  private long duration;

  /**
   * @param jobName
   * @param startTime
   */
  public JobStat() {
  }


  /**
   * @param jobName
   * @param jobId
   */
  public JobStat(String jobName, UUID jobId) {
    super();
    this.jobName = jobName;
    this.jobId = jobId;
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
  public long getStartTime() {
    return startTime;
  }

  /**
   * Don't set this, it won't accomplish anything.  This is overwritten by the job as an audit record
   * @param startTime
   *          the fireTime to set
   */
  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }
  
  public void incrementRuns(){
    runCount++;
  }
  
  /**
   * Get the number of times this job has failed
   * @return
   */
  public int getRunCount(){
    return runCount;
  }

  /**
   * @param failCount the failCount to set
   */
  public void setRunCount(int runCount) {
    this.runCount = runCount;
  }


  /**
   * @return the duration
   */
  public long getDuration() {
    return duration;
  }


  /**
   * @param duration the duration to set
   */
  public void setDuration(long duration) {
    this.duration = duration;
  }


  /**
   * @return the jobId
   */
  public UUID getJobId() {
    return jobId;
  }


  /**
   * @param jobId the jobId to set
   */
  public void setJobId(UUID jobId) {
    this.jobId = jobId;
  }

}
