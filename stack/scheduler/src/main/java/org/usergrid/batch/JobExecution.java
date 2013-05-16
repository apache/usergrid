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
package org.usergrid.batch;

import java.util.UUID;

import org.usergrid.persistence.entities.JobData;
import org.usergrid.persistence.entities.JobStat;

/**
 * Interface to define all operations possible during a job execution. The job
 * execution has several states.
 * 
 * 
 * The Execution has the following state transitions
 * 
 * NOT_STARTED->IN_PROGRESS
 * 
 * 
 * IN_PROGRESS->COMPLETED <br/>
 * IN_PROGRESS->FAILED <br/>
 * IN_PROGRESS->DEAD
 * 
 * FAILED->IN_PROGRESS
 * 
 * 
 * @author tnine
 * 
 */
public interface JobExecution {

  /**
   * Retry constant to signal the job should try forever
   */
  public static final int FOREVER = -1;

  /**
   * Get the data for this execution
   * 
   * @return
   */
  public JobData getJobData();

  /**
   * Get the job statistic information
   * 
   * @return
   */
  public JobStat getJobStats();

  /**
   * Marke the job as started.  If it's failed too many times, don't run it
   */
  public void start(int maxFailures);

  /**
   * Mark the job as successfully completed
   */
  public void completed();

  /**
   * Mark the job as failed. If it has failed more than maxFailures, mark it as
   * dead
   * 
   * @param maxFailures
   */
  public void failed();

  /**
   * Mark the job as dead
   */
  public void killed();

  /**
   * Provide a heartbeat to the job execution to keep it alive
   */
  public void heartbeat();
  
  /**
   * Signal the execution is still running, and delay the timeout for the milliseconds specified
   * 
   * @param milliseconds
   */
  public void heartbeat(long milliseconds);
  
  /**
   * Don't treat the execution as complete.  Simply delay execution for the specified milliseconds.  Similar to heartbeat
   * but allows the user to specify the timeout for the next attempt instead of the heartbeat default.  This DOES NOT update 
   * locks, so your job should use distributed locking internally to ensure single execution
   * 
   * @param milliseconds
   */
  public void delay(long milliseconds);

  /**
   * Get the current status of the execution
   * 
   * @return
   */
  public Status getStatus();

  /**
   * Get the name of the job
   * 
   * @return
   */
  public String getJobName();

  /**
   * Get the job id
   * 
   * @return
   */
  public UUID getJobId();

  /**
   * Get the current transaction Id from the heartbeat
   * 
   * @return
   */
  public UUID getTransactionId();

  public enum Status {
    NOT_STARTED, IN_PROGRESS, COMPLETED, FAILED, DEAD, DELAYED
  }

}
