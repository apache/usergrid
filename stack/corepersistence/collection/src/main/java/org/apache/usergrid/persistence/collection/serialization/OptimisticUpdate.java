package org.apache.usergrid.persistence.collection.serialization;


import java.util.UUID;

import org.apache.usergrid.persistence.collection.MvccEntity;


/**
 * Interface to define how optimistic updates should be performed
 */
public interface OptimisticUpdate {

    /**
     * WriteUniqueVerify the entity we're trying to write in our current context has the correct most current version
     *
     * @param context The mvcc context
     * @param optimisticVersion The optimistic version the caller provider as the most up to date
     *
     * @return True if the optimisticVersion is the most current >= Comitted stage, false otherwise
     */
    public boolean verifyCurrent( MvccEntity context, UUID optimisticVersion );
}
