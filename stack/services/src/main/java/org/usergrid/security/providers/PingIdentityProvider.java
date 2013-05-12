package org.usergrid.security.providers;

import org.usergrid.management.ManagementService;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.entities.User;
import org.usergrid.security.tokens.exceptions.BadTokenException;

import java.util.Map;
import java.util.UUID;

/**
 * Provider implementation for accessing Ping Identity
 *
 * @author zznate
 */
public class PingIdentityProvider extends AbstractProvider {


  PingIdentityProvider(EntityManager entityManager, ManagementService managementService) {
    super(entityManager, managementService);
  }

  @Override
  public User createOrAuthenticate(String externalToken) throws BadTokenException {
    return null;
  }

  @Override
  void configure() {
    // TODO
  }

  @Override
  public Map<Object, Object> loadConfigurationFor() {
    return loadConfigurationFor("pingIdentProvider");
  }

  @Override
  public void saveToConfiguration(Map<String, Object> config) {
    saveToConfiguration("pingIdentProvider", config);
  }
}
