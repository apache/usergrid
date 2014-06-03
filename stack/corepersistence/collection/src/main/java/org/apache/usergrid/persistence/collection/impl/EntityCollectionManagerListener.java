package org.apache.usergrid.persistence.collection.impl;


import org.apache.usergrid.persistence.collection.guice.Write;
import org.apache.usergrid.persistence.collection.guice.WriteUpdate;
import org.apache.usergrid.persistence.collection.mvcc.stage.EntityUpdateEvent;
import org.apache.usergrid.persistence.collection.mvcc.stage.load.Load;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteStart;
import org.apache.usergrid.persistence.core.consistency.AsyncProcessorFactory;
import org.apache.usergrid.persistence.core.consistency.MessageListener;
import org.apache.usergrid.persistence.model.entity.Entity;

import rx.Observable;
import rx.schedulers.Schedulers;


/**
 *
 *
 */
public class EntityCollectionManagerListener implements MessageListener<EntityUpdateEvent,Entity> {

    Load load;
    WriteStart writeStart;
    WriteStart writeUpdate;

    public EntityCollectionManagerListener(
                                           final Load load,
                                           @Write final WriteStart writeStart,
                                           @WriteUpdate final WriteStart writeUpdate,
                                           final AsyncProcessorFactory asyncProcessorFactory){
        this.load = load;
        this.writeStart = writeStart;
        this.writeUpdate = writeUpdate;
        asyncProcessorFactory.getProcessor( EntityUpdateEvent.class ).addListener( this );

    }

    @Override
    public Observable<Entity> receive( final EntityUpdateEvent event) {

        return Observable.from(event)
                  .subscribeOn( Schedulers.io() ).map( load );
    }

}
