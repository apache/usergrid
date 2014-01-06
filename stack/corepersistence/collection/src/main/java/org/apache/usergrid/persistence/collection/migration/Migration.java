package org.apache.usergrid.persistence.collection.migration;


import java.util.Collection;

import org.apache.usergrid.persistence.collection.astyanax.MultiTennantColumnFamilyDefinition;


/**
 * @author tnine
 */
public interface Migration {

    /**
     * Get the column families required for this implementation.  If one does not exist it will be created.
     */
    public Collection<MultiTennantColumnFamilyDefinition> getColumnFamilies();
}
