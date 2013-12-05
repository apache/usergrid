package org.apache.usergrid.persistence.collection.mvcc.stage;


import com.google.common.eventbus.Subscribe;


/** The possible stages in our write flow. */
public interface EventStage<T extends CollectionEvent> {

    /**
     * Run this stage.  This will return the MvccEntity that should be returned or passed to the next stage
     *
     * @param event The event to receive
     *
     */
    @Subscribe
    public void performStage(T event );
}
