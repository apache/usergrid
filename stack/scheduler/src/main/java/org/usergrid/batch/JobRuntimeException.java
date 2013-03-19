package org.usergrid.batch;


/**
 * Created when a job cannot be instantiated.  This usually occurs during the deploy of new code on nodes that don't yet have the job
 * implementation.  Nodes receiving this message should log it and move on.
 * @author tnine
 */
public class JobRuntimeException extends RuntimeException {

  /**
   * 
   */
  private static final long serialVersionUID = 1;

  /**
   * 
   */
  public JobRuntimeException() {
    super();
  }

  /**
   * @param arg0
   * @param arg1
   */
  public JobRuntimeException(String arg0, Throwable arg1) {
    super(arg0, arg1);
  }

  /**
   * @param arg0
   */
  public JobRuntimeException(String arg0) {
    super(arg0);
  }

  /**
   * @param arg0
   */
  public JobRuntimeException(Throwable arg0) {
    super(arg0);
  }

  
 
}
