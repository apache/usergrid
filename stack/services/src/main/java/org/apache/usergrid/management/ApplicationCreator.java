package org.apache.usergrid.management;


import org.apache.usergrid.management.exceptions.ApplicationCreationException;


/**
 * Methods for marshalling application creation logic
 *
 * @author zznate
 */
public interface ApplicationCreator {
    ApplicationInfo createSampleFor( OrganizationInfo organizationInfo ) throws ApplicationCreationException;
}
