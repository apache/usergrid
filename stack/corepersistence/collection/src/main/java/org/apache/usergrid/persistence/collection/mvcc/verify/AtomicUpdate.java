package org.apache.usergrid.persistence.collection.mvcc.verify;


import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;


/**
 * Interface to test if we can perform atomic operations
 * <p/>
 * Note This will probably require a new WriteStage that is after start, which is rollback
 */
public interface AtomicUpdate
{

    /** Signal that we are starting update */
    public void startUpdate( MvccEntity context );

    /**
     * Try the commit.
     *
     * @return true if we can proceed.  False if we cannot
     */
    public boolean tryCommit( MvccEntity context );
}


