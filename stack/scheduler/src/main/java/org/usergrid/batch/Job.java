package org.usergrid.batch;


/**
 * Defines only an execute method. Implementation functionality is completely up
 * to the {@link JobFactory}
 * 
 * @author zznate
 */
public interface Job {

  /**
   * Invoked when a job should execute
   * 
   * @param execution
   *          The execution information
   * @throws JobExecutionException
   *           If the job cannot be executed
   */
  public void execute(JobExecution execution) throws JobExecutionException;

}
