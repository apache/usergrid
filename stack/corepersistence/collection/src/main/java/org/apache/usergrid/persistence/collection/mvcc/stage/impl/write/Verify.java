package org.apache.usergrid.persistence.collection.mvcc.stage.impl.write;


import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.IoEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;
import rx.util.functions.Func1;


/** This phase should execute any verification on the MvccEntity */
@Singleton
public class Verify implements Func1<IoEvent<MvccEntity>, Observable<IoEvent<MvccEntity>>> {

   @Inject
    public Verify( ) {
    }


    @Override
    public Observable<IoEvent<MvccEntity>> call( final IoEvent<MvccEntity> mvccEntityIoEvent ) {
        //no op, just emit the new obsevable
        return Observable.from( mvccEntityIoEvent );
    }
}
