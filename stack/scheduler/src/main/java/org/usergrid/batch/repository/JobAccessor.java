package org.usergrid.batch.repository;

import java.util.List;

import org.usergrid.batch.JobExecution;

/**
 * @author zznate
 */
public interface JobAccessor {

  /**
   * Get new jobs, with a max return value of size
   * @param size
   * @return
   */
  public List<JobDescriptor> getJobs(int size);

  /**
   * Save job execution information
   * @param bulkJobExecution
   */
  public void save(JobExecution bulkJobExecution);
  
  /**
   * Don't remove the execution, but rather schedule it to be fired after the given delay
   * @param execution
   * @param delay
   */
  public void delayRetry(JobExecution execution, long delay);
  
}
