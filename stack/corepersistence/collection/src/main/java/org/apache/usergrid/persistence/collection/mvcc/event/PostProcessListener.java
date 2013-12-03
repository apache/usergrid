package org.apache.usergrid.persistence.collection.mvcc.event;


import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;


/**
 *
 * @author: tnine
 *
 */
public interface PostProcessListener<T>
{


    /**
     * The entity was rejected by the MVCC system and will be removed
     *
     * @param data The data used in the write pipeline
     * @return the MvccEntity to use during this stage
     */
    public T doPostProcessing(T data );

}
