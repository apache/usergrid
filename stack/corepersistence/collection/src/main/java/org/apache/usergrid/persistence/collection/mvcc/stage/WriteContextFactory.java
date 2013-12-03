package org.apache.usergrid.persistence.collection.mvcc.stage;


import java.util.UUID;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.model.entity.Entity;


/** @author tnine */
public interface WriteContextFactory {

    /**
     * Return a new write context for the given stage pipeline
     * @param
     * @return A write context that can be used for creating entities.  Returns the new entity to use after
     * the write has completed
     */
    WriteContext newCreateContext(CollectionContext context);

    /**
     * Create a write context that cna be used for deleting entitie
     * @return
     */
    WriteContext newDeleteContext(CollectionContext context);



}
