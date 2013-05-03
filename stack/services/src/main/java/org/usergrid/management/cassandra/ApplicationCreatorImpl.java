package org.usergrid.management.cassandra;

import java.util.Arrays;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.management.ApplicationCreator;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.ManagementService;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.exceptions.ApplicationCreationException;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.EntityManagerFactory;

import com.google.common.base.Preconditions;

/**
 * @author zznate
 */
public class ApplicationCreatorImpl implements ApplicationCreator {

    public static final String DEF_SAMPLE_APP_NAME = "sandbox";

    private static final Logger logger = LoggerFactory.getLogger(ApplicationCreatorImpl.class);

    private final ManagementService managementService;
    private final EntityManagerFactory entityManagerFactory;
    private String sampleAppName = DEF_SAMPLE_APP_NAME;

    public ApplicationCreatorImpl(EntityManagerFactory entityManagerFactory,
            ManagementService managementService) {
        this.entityManagerFactory = entityManagerFactory;
        this.managementService = managementService;
    }

    public void setSampleAppName(String sampleAppName) {
        this.sampleAppName = sampleAppName;
    }

    @Override
    public ApplicationInfo createSampleFor(OrganizationInfo organizationInfo)
            throws ApplicationCreationException {

        Preconditions.checkArgument(organizationInfo != null,
                "OrganizationInfo was null");
        Preconditions.checkArgument(organizationInfo.getUuid() != null,
                "OrganizationInfo had no UUID");
		logger.info("create sample app {} in: {}", sampleAppName, organizationInfo.getName());
        UUID appId = null;
        try {
            appId = managementService.createApplication(
                    organizationInfo.getUuid(), sampleAppName).getId();
        } catch (Exception ex) {
            throw new ApplicationCreationException("'" + sampleAppName
                    + "' could not be created for organization: "
                    + organizationInfo.getUuid(), ex);
        }
        logger.info("granting permissions for: {} in: {}", sampleAppName, organizationInfo.getName());
        // grant access to all default collections with groups
        EntityManager em = entityManagerFactory.getEntityManager(appId);
        try {
            em.grantRolePermissions("guest",
                    Arrays.asList("get,post,put,delete:/**"));
            em.grantRolePermissions("default",
                    Arrays.asList("get,put,post,delete:/**"));
        } catch (Exception ex) {
            throw new ApplicationCreationException(
                    "Could not grant permissions to guest for default collections in '"
                            + sampleAppName + "'", ex);
        }
        // re-load the applicationinfo so the correct name is set
        try {
            return managementService.getApplicationInfo(appId);
        } catch (Exception ex) {
            throw new ApplicationCreationException(
                    "Could not load new Application.", ex);
        }
    }

}
