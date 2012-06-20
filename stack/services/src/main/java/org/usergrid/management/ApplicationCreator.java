package org.usergrid.management;

import org.usergrid.management.exceptions.ApplicationCreationException;

/**
 * Methods for marshalling application creation logic
 *
 * @author zznate
 */
public interface ApplicationCreator {
  ApplicationInfo createSampleFor(OrganizationInfo organizationInfo)
          throws ApplicationCreationException;

}
