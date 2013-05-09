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

import static org.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.usergrid.batch.Job;
import org.usergrid.batch.JobExecution;
import org.usergrid.locking.Lock;
import org.usergrid.locking.LockManager;

/**
 * Simple abstract job class that performs additional locking to ensure that the
 * job is only executing once. This can be used if your job could potentially be
 * too slow to invoke JobExceution.heartbeat() before the timeout passes.
 * 
 * @author tnine
 * 
 */
@Component("OnlyOnceJob")
public abstract class OnlyOnceJob implements Job {

  @Autowired
  private LockManager lockManager;

  /**
   * 
   */
  public OnlyOnceJob() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.usergrid.batch.Job#execute(org.usergrid.batch.JobExecution)
   */
  @Override
  public void execute(JobExecution execution) throws Exception {

    String lockId = execution.getJobId().toString();

    Lock lock = lockManager.createLock(MANAGEMENT_APPLICATION_ID, String.format("/jobs/%s", lockId));

    // the job is still running somewhere else. Try again in 10 seconds
    if (!lock.tryLock(0, TimeUnit.MILLISECONDS)) {
      execution.delay(getDelay(execution));
      return;
    }
    
    //if we get here we can proceed.  Make sure we unlock no matter what.
    try {

      doJob(execution);

    } finally {
      lock.unlock();
    }

  }

  /**
   * Delegate the job execution to the subclass
   * 
   * @param execution
   * @throws Exception 
   */
  protected abstract void doJob(JobExecution execution) throws Exception;
  
  /**
   * Get the delay for the next run if we can't acquire the lock
   * @param execution
   * @return
   * @throws Exception
   */
  protected abstract long getDelay(JobExecution execution) throws Exception;

}
