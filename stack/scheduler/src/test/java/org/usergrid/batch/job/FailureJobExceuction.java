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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Ignore;
import org.springframework.stereotype.Component;
import org.usergrid.batch.Job;
import org.usergrid.batch.JobExecution;
import org.usergrid.batch.JobExecutionException;

/**
 * A simple job that does nothing but increment an atomic counter
 * 
 * @author tnine
 * 
 */
@Component("failureJobExceuction")
@Ignore("Not a test")
public class FailureJobExceuction implements Job {

  protected int count;

  /**
   * 
   */
  public FailureJobExceuction() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.usergrid.batch.Job#execute(org.usergrid.batch.JobExecution)
   */
  @Override
  public void execute(JobExecution execution) throws JobExecutionException {
    execution.start();

    count++;

    throw new JobExecutionException(execution, "Failed", null);
  }

  public void reset() {
    count = 0;
  }

  public int getCount() {
    return count;
  }

}
