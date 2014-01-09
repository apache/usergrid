package org.apache.usergrid.persistence.collection.rx;


import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.concurrency.Schedulers;
import rx.operators.OperationMerge;
import rx.util.functions.Func1;


/**
 * A utility class that encapsulates many Funct1 operations that receive and emit the same type.
 * These functions are executed in parallel, then "zipped" into a single response.  This is useful
 * when you want to perform operations on a single initial observable, then combine the result into a
 * single observable to continue the sequence

 */
public class Concurrent<T, R> implements Func1<T, Observable<R>> {



    private final Func1<T, R>[] concurrent;

    private Concurrent( final Func1<T, R>[] concurrent ){
        this.concurrent = concurrent;
    }

    @Override
      public Observable<R> call( final T input ) {

        List<Observable<R>> observables = new ArrayList<Observable<R>>(concurrent.length);

        //put all our observables together for concurrency
        for( Func1<T, R> funct: concurrent){
            final Observable<R> observable = Observable.from(input).subscribeOn(  Schedulers.threadPoolForIO() ).map( funct );

            observables.add( observable );
        }




        final Observable.OnSubscribeFunc<R> merge = OperationMerge.merge( observables );
        final Observable<R> newObservable = Observable.create( merge );


        //wait until the last operation completes to proceed
        return newObservable.takeLast( 1 );

      }


    /**
     * Create an instance of concurrent execution.  All functions specified in the list are invoked in parallel.
     * The results are then "zipped" into a single observable which is returned
     *
     *
     * @param observable The observable we're invoking on
     * @param concurrent The concurrent operations we're invoking
     * @return
     */
    public static <T, R> Observable<R> concurrent( final Observable<T> observable, final Func1<T, R>... concurrent ){
          return observable.mapMany( new Concurrent<T, R>( concurrent ));
    }


}
