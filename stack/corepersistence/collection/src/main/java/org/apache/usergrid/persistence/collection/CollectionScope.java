package org.apache.usergrid.persistence.collection;


import org.apache.usergrid.persistence.model.entity.Id;


/**
 * A scope to use when creating the collection manager.  Typically, this would be something like an application, or an
 * organization. Data encapsulated within instances of a scope are mutually exclusive from instances with other ids and
 * names.
 */
public interface CollectionScope extends OrganizationScope {

    /**
     * @return The name of the collection. If you use pluralization for you names vs types,
     * you must keep the consistent or you will be unable to load data
     */
    public String getName();


    /**
     * @return A uuid that is unique to this context.  It can be any uuid (time uuid preferred).  Usually an application
     *         Id, but could be an entity Id that is the parent of another collection
     */
    public Id getOwner();
}
