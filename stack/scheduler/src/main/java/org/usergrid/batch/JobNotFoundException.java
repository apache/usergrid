package org.usergrid.batch;


/**
 * Created when a job cannot be instantiated.  This usually occurs during the deploy of new code on nodes that don't yet have the job
 * implementation.  Nodes receiving this message should log it and move on.
 * @author tnine
 */
public class JobNotFoundException extends Exception {

  /**
   * 
   */
  private static final long serialVersionUID = -67437852214725320L;

  private static final String DEF_MSG = "Unable to find the job with name %s";

  public JobNotFoundException(String jobName) {
    super(String.format(DEF_MSG, jobName));

  }
}
