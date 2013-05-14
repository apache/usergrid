package org.usergrid.security.providers;

import org.springframework.beans.factory.annotation.Autowired;
import org.usergrid.management.ManagementService;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.entities.Application;

import java.util.Map;

/**
 * @author zznate
 */
public class SignInProviderFactory {

  private EntityManagerFactory emf;
  private ManagementService managementService;

  @Autowired
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Autowired
  public void setManagementService(ManagementService managementService) {
    this.managementService = managementService;
  }

  public SignInAsProvider facebook(Application application) {
    FacebookProvider facebookProvider =
            new FacebookProvider(emf.getEntityManager(application.getUuid()), managementService);
    facebookProvider.configure();
    return facebookProvider;
  }

  public SignInAsProvider foursquare(Application application){
    FoursquareProvider foursquareProvider = new FoursquareProvider(emf.getEntityManager(application.getUuid()),
            managementService);
    foursquareProvider.configure();
    return foursquareProvider;
  }

  public SignInAsProvider pingident(Application application) {
    PingIdentityProvider pingIdentityProvider = new PingIdentityProvider(emf.getEntityManager(application.getUuid()),
            managementService);
    pingIdentityProvider.configure();
    return pingIdentityProvider;
  }




}
