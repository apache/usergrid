package org.apache.usergrid.persistence.collection.serialization;


import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.model.field.Field;


/**
 * Interface to define how unique updates should be performed
 */
public interface UniqueUpdate {

    /**
     * WriteUniqueVerify the entity we're trying to write in our current context has the correct most current version
     *
     * @param context The mvcc context
     * @param uniqueField The field to check for uniqueness
     *
     * @return True if the value in the uniqueField is unique in the collection context
     */
    public boolean verifyUnique( MvccEntity context, Field<?> uniqueField );

    /**
     * During the commit phase, ensure this entity is committed as a unique value. This may release locks or overwrite
     * expiring timeout values since we are at the final commit phase
     */
    public void commitUnique( MvccEntity entity, Field<?> uniqueField );
}
