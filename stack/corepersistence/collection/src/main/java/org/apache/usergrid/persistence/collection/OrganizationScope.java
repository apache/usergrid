package org.apache.usergrid.persistence.collection;


import org.apache.usergrid.persistence.model.entity.Id;


/**
 * A scope used for organizations
 */
public interface OrganizationScope {

    /**
     * Get an organization scope
     */
    Id getOrganization();
}
