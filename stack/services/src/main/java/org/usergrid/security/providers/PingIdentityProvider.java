package org.usergrid.security.providers;

import org.usergrid.persistence.entities.User;
import org.usergrid.security.tokens.exceptions.BadTokenException;

import java.util.UUID;

/**
 * Provider implementation for accessing Ping Identity
 *
 * @author zznate
 */
public class PingIdentityProvider implements SignInAsProvider {
  @Override
  public User createOrAuthenticate(UUID userId, String externalToken) throws BadTokenException {
    return null;
  }

}
