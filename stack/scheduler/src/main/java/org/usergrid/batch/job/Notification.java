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
package org.usergrid.batch.job;

import org.springframework.stereotype.Component;
import org.usergrid.batch.Job;
import org.usergrid.batch.JobExecution;
import org.usergrid.batch.JobExecutionException;

/**
 * @author tnine
 * 
 */
@Component("notification")
public class Notification implements Job {

  /**
   * 
   */
  public Notification() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.usergrid.batch.Job#execute(org.usergrid.batch.JobExecution)
   */
  @Override
  public void execute(JobExecution execution) throws JobExecutionException {
    // TODO, 1 page of data, then invoke the call below. Must be invoked every
    // 30 seconds to avoid this job timing out as dead

    try {
      
      while (true) {
        // do 1 execution, less than 30 seconds
        
        //signal we're still processing
        execution.heartbeat();
      }
    } 
    //if ANY error occurs, we want to throw an execution exception.  This will mark the job as failed, 
    // and cause it to re-fire. We want to be able to mark any error as a
    // failure, so we can eventually stop firing the job and stop it from 
    //being re-scheduled indefinitely
    catch (Throwable t) {
      throw new JobExecutionException(execution, "Unable to run job", t);
    }

 
  }

}
