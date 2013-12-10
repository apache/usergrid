package org.apache.usergrid.persistence.collection;


import org.apache.usergrid.persistence.model.entity.Id;


/**
 * A scope to use when encapsulating any data within a scope
 */
public interface Scope {

    /**
     * @return A uuid that is unique to this context.  It can be any uuid (time uuid preferred).  Usually an application
     *         Id, but could be an entity Id that is the parent of another collection
     */
    public Id getOwner();

}
