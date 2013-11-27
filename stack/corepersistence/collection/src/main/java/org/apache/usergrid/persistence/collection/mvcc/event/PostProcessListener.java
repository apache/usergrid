package org.apache.usergrid.persistence.collection.mvcc.event;


import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;


/**
 *
 * @author: tnine
 *
 */
public interface PostProcessListener<T extends MvccEntity>
{


    /**
     * The entity was rejected by the MVCC system and will be removed
     *
     * @param mvccEntity The mvcc entity to perform post processing on
     * @return the MvccEntity to use during this stage
     */
    public MvccEntity doPostProcessing(MvccEntity mvccEntity );

}
