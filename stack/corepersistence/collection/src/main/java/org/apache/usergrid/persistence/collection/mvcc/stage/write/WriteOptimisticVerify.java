package org.apache.usergrid.persistence.collection.mvcc.stage.write;


import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.stage.IoEvent;
import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.util.functions.Func1;


/**
 * This phase should execute any optimistic verification on the MvccEntity
 */
@Singleton
public class WriteOptimisticVerify implements Func1<IoEvent<MvccEntity>, IoEvent<MvccEntity>> {

    @Inject
    public WriteOptimisticVerify() {
    }


    @Override
    public IoEvent<MvccEntity> call( final IoEvent<MvccEntity> mvccEntityIoEvent ) {
        ValidationUtils.verifyMvccEntityWithEntity( mvccEntityIoEvent.getEvent() );

        //no op, just emit the value
        return mvccEntityIoEvent;
    }
}
