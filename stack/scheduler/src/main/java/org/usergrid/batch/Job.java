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
   *          The execution information.  This will be the same from the last run.  By default you should call exeuction.start() once processing starts
   * @throws JobExecutionException
   *           If the job cannot be executed
   */
  public void execute(JobExecution execution) throws JobExecutionException;

}
