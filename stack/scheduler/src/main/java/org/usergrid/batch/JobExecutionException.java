package org.usergrid.batch;

/**
 * @author zznate
 * @author tnine
 */
public class JobExecutionException extends Exception {

  /**
   * 
   */
  private static final long serialVersionUID = -67437852214725320L;

  private static final String DEF_MSG = "There was a problem executing the bulk job: ";

  
  private final long retryTimeout;

  /**
   * Set the exception with the retry timeout and the String message
   * @param jobExecution The job execution instance
   * @param retryTimeout The time in millis to time out if we fail
   * @param message The message to display
   * @param t The parent cause of this exception 
   */
  public JobExecutionException(long retryTimeout, String message, Throwable t) {
    super(DEF_MSG + message, t);
    this.retryTimeout = retryTimeout;
  }
  
  /**
   * Set the error with the retry time to fire
   * @param jobExecution
   * @param message
   * @param t
   */
  public JobExecutionException(String message, Throwable t) {
    this(0, message, t);
  }


  /**
   * @return the retryTimeout
   */
  public long getRetryTimeout() {
    return retryTimeout;
  }

}
