package org.usergrid.security.providers;

import org.usergrid.persistence.entities.User;
import org.usergrid.security.tokens.exceptions.BadTokenException;

import java.util.UUID;

/**
 * @author zznate
 */
public interface SignInAsProvider {

  /**
   * Authenticate a userId and external token against this provider
   * @param externalToken
   * @return
   */
  User createOrAuthenticate(String externalToken) throws BadTokenException;

}
