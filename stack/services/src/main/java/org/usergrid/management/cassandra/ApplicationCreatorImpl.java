package org.usergrid.management.cassandra;

import org.usergrid.management.ApplicationCreator;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.ManagementService;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.exceptions.ApplicationCreationException;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.entities.Application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author zznate
 */
public class ApplicationCreatorImpl implements ApplicationCreator {

  private final ManagementService managementService;
  private final EntityManagerFactory entityManagerFactory;

  public ApplicationCreatorImpl(EntityManagerFactory entityManagerFactory,
                                ManagementService managementService) {
    this.entityManagerFactory = entityManagerFactory;
    this.managementService = managementService;
  }

  @Override
  public ApplicationInfo createSampleFor(OrganizationInfo organizationInfo) throws ApplicationCreationException {
    UUID appId = null;
    try {
      appId = managementService.createApplication(organizationInfo.getUuid(), "sandbox");
    } catch (Exception ex) {
      throw new ApplicationCreationException("'sandbox' could not be created for organization: "
              + organizationInfo.getUuid(), ex);
    }
    // grant access to all default collections with groups
    EntityManager em = entityManagerFactory.getEntityManager(appId);
    try {
      em.grantRolePermissions("guest", Arrays.asList("get,post:/**"));
    } catch (Exception ex) {
      throw new ApplicationCreationException("Could not grant permissions to guest for default collections in 'sandbox'", ex);
    }
    // re-load the applicationinfo so the correct name is set
    try {
      return managementService.getApplicationInfo(appId);
    } catch (Exception ex) {
      throw new ApplicationCreationException("Could not load new Application.", ex);
    }
  }


}
