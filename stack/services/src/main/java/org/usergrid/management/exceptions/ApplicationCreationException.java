package org.usergrid.management.exceptions;

/**
 * Typed exception for application creation failures
 * @author zznate
 */
public class ApplicationCreationException extends ManagementException {
  private static final String DEF_MSG =
          "There was a problem creating the application: ";

  public ApplicationCreationException() {
    super(DEF_MSG);
  }

  public ApplicationCreationException(String msg) {
    super(DEF_MSG + msg);
  }

  public ApplicationCreationException(String msg, Throwable t) {
    super(DEF_MSG + msg, t);
  }

}
