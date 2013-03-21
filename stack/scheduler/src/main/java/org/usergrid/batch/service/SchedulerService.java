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

import java.util.UUID;

import org.usergrid.batch.JobExecutionException;
import org.usergrid.batch.JobExecutionImpl;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.entities.JobData;

/**
 * Simple interface for performing job scheduling
 * 
 * @author tnine
 * 
 */
public interface SchedulerService {

  /**
   * Create a new job
   * 
   * @param jobName The name of the job.  There must be an implentation in the spring context of type org.usergrid.batch.Job with the name
   * @param fireTime The time to fire in milliseconds since epoch
   * @param jobData The data to pass to the job
   * 
   * @return The newly created job data.  The job data uuid is the job id
   * @throws Exception 
   * 
   */
  public JobData createJob(String jobName, long fireTime, JobData jobData);

  /**
   * Delete the job.
   * 
   * @param jobId
   */
  public void deleteJob(UUID jobId);
  
  /**
   * Perform any heartbeat operations required.  Update jobExecution with the appropriate data
   * @param execution
   * @throws JobExecutionException 
   */
  public void heartbeat(JobExecutionImpl execution);
  
  /**
   * Query the job data with the given query object
   * @param query
   * @return
   * @throws Exception 
   */
  public Results queryJobData(Query query) throws Exception;
}
