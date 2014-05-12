package org.apache.usergrid.persistence.collection.impl;


import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.guice.EntityUpdate;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.collection.mvcc.stage.load.Load;
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
    CollectionScope context;

    public EntityCollectionManagerListener(
                                           Load load,
                                           @EntityUpdate final AsyncProcessor entityUpdate){
        this.load = load;
        entityUpdate.addListener( this );

    }

    @Override
    public Observable<Entity> receive( final CollectionIoEvent<Id> placeholder) {

        return Observable.from(placeholder )
                  .subscribeOn( Schedulers.io() ).map( load );
    }

}
