package org.apache.usergrid.persistence.collection.mvcc.event;


import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;


/**
 *
 * @author: tnine
 *
 */
public interface PostProcessObserver {


    /**
     * The entity was comitted by the MVCC system.  Post processing needs to occur
     *
     * @param scope The scope used in the write pipeline
     * @param entity The entity used in the write pipeline
     *
     */
    public void postCommit(CollectionScope scope,  MvccEntity entity );
}
