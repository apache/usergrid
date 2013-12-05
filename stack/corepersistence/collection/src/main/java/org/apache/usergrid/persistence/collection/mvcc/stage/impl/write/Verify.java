package org.apache.usergrid.persistence.collection.mvcc.stage.impl.write;


import org.apache.usergrid.persistence.collection.mvcc.entity.CollectionEventBus;
import org.apache.usergrid.persistence.collection.mvcc.stage.EventStage;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/** This phase should execute any verification on the MvccEntity */
@Singleton
public class Verify implements EventStage<EventVerify> {

    private final CollectionEventBus eventBus;

    @Inject
    public Verify( final CollectionEventBus eventBus ) {
        this.eventBus = eventBus;
        this.eventBus.register( this );
    }



    @Override
    public void performStage( final EventVerify event ) {
        //no op, verification needs to happen here

        eventBus.post( new EventCommit(event.getCollectionContext(), event.getData(), event.getResult()) );
    }
}
