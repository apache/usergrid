package org.usergrid.management.exceptions;

public class RecentlyUsedPasswordException extends ManagementException {

  private static final long serialVersionUID = 1L;

  public RecentlyUsedPasswordException() {
    super();
  }

  public RecentlyUsedPasswordException(String arg0, Throwable arg1) {
    super(arg0, arg1);
  }

  public RecentlyUsedPasswordException(String arg0) {
    super(arg0);
  }

  public RecentlyUsedPasswordException(Throwable arg0) {
    super(arg0);
  }
}
