package org.apache.usergrid.persistence.collection;


import org.apache.usergrid.persistence.model.entity.Id;


/**
 * A scope to use when creating the collection manager.  Typically, this would be something like an application, or an
 * organization.  Data encapsulated within instances of a scope are mutually exclusive from instances with other ids and
 * names.
 */
public interface CollectionScope extends Scope {

    /** @return The name of the collection. If you use pluralization for you names vs types,
     * you must keep the consistent or you will be unable to load data
     * @return
     */
    public String getName();
}
