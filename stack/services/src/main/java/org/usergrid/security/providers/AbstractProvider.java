package org.usergrid.security.providers;

import org.springframework.beans.factory.annotation.Autowired;
import org.usergrid.management.ManagementService;
import org.usergrid.persistence.EntityManagerFactory;

/**
 * @author zznate
 */
public abstract class AbstractProvider implements SignInAsProvider {

  protected EntityManagerFactory emf;
  protected ManagementService managementService;

  @Autowired
  public void setEmf(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Autowired
  public void setManagementService(ManagementService managementService) {
    this.managementService = managementService;
  }

}
