package org.usergrid.rest.exceptions;

import static javax.ws.rs.core.Response.Status.CONFLICT;
import org.usergrid.management.exceptions.RecentlyUsedPasswordException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * <p>
 * Map an RecentlyUsedPasswordException to an HTTP 409 response.
 * </p>
 */
@Provider
public class RecentlyUsedPasswordExceptionMapper
    extends AbstractExceptionMapper<RecentlyUsedPasswordException> {

  @Override
  public Response toResponse(RecentlyUsedPasswordException e) {

    return toResponse(CONFLICT, e);
  }
}
