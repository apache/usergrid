package org.usergrid.batch;

/**
 * @author zznate
 */
public class JobExecutionException extends Exception {

  /**
   * 
   */
  private static final long serialVersionUID = -67437852214725320L;

  private static final String DEF_MSG = "There was a problem executing the bulk job: ";

  private final JobExecution bulkJobExecution;

  public JobExecutionException(JobExecution bulkJobExecution, String message, Throwable t) {
    super(DEF_MSG + message, t);
    this.bulkJobExecution = bulkJobExecution;
  }

  public JobExecution getExecution() {
    return this.bulkJobExecution;
  }

}
