package org.apache.usergrid.persistence.collection.mvcc.stage.write;


import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.functions.Func1;


/**
 * This phase should execute any optimistic verification on the MvccEntity
 */
@Singleton
public class WriteOptimisticVerify implements Func1<CollectionIoEvent<MvccEntity>, CollectionIoEvent<MvccEntity>> {

    @Inject
    public WriteOptimisticVerify() {
    }


    @Override
    public CollectionIoEvent<MvccEntity> call( final CollectionIoEvent<MvccEntity> mvccEntityIoEvent ) {
        ValidationUtils.verifyMvccEntityWithEntity( mvccEntityIoEvent.getEvent() );

        //no op, just emit the value
        return mvccEntityIoEvent;
    }
}
