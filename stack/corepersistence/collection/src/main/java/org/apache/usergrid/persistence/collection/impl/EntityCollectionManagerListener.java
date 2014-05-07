package org.apache.usergrid.persistence.collection.impl;


import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
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
public class EntityCollectionManagerListener implements MessageListener<Entity,Entity> {

    EntityCollectionManager entityCollectionManager;
    Load load;
    CollectionScope context;

    public EntityCollectionManagerListener(CollectionScope context,
                                           Load load,
                                           @EntityUpdate final AsyncProcessor entityUpdate){
        this.context = context;
        this.load = load;

        entityUpdate.addListener( this );

    }

    @Override
    public Observable<Entity> receive( final Entity event ) {
        return Observable.from( new CollectionIoEvent<Id>( context, event.getId() ) )
                  .subscribeOn( Schedulers.io() ).map( load );
    }
}
