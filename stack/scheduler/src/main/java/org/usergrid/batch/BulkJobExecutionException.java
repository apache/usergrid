package org.usergrid.batch;

/**
 * @author zznate
 */
public class BulkJobExecutionException extends Exception {

  /**
   * 
   */
  private static final long serialVersionUID = -67437852214725320L;

  private static final String DEF_MSG = "There was a problem executing the bulk job: ";

  private final BulkJobExecution bulkJobExecution;

  public BulkJobExecutionException(BulkJobExecution bulkJobExecution, String message, Throwable t) {
    super(DEF_MSG + message, t);
    this.bulkJobExecution = bulkJobExecution;
  }

  public BulkJobExecution getBulkJobExecution() {
    return this.bulkJobExecution;
  }

}
