package org.apache.usergrid.persistence.collection.impl;


import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.guice.EntityUpdate;
import org.apache.usergrid.persistence.collection.guice.Write;
import org.apache.usergrid.persistence.collection.guice.WriteUpdate;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.mvcc.stage.load.Load;
import org.apache.usergrid.persistence.collection.mvcc.stage.write.WriteStart;
import org.apache.usergrid.persistence.core.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.core.consistency.MessageListener;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import rx.Observable;
import rx.schedulers.Schedulers;


/**
 *
 *
 */
public class EntityCollectionManagerListener implements MessageListener<CollectionIoEvent<Id>,Entity> {

    Load load;
    WriteStart writeStart;
    WriteStart writeUpdate;
    CollectionScope context;

    public EntityCollectionManagerListener(
                                           Load load,
                                           @Write final WriteStart writeStart,
                                           @WriteUpdate final WriteStart writeUpdate,
                                           @EntityUpdate final AsyncProcessor entityUpdate){
        this.load = load;
        this.writeStart = writeStart;
        this.writeUpdate = writeUpdate;
        entityUpdate.addListener( this );

    }

    @Override
    public Observable<Entity> receive( final CollectionIoEvent<Id> placeholder) {

        return Observable.from(placeholder )
                  .subscribeOn( Schedulers.io() ).map( load );
    }

}
