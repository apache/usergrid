package org.apache.usergrid.persistence.collection.rx;


import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Scheduler;
import rx.util.functions.Func1;
import rx.util.functions.FuncN;


/**
 * A utility class that encapsulates many Funct1 operations that receive and emit the same type.
 * These functions are executed in parallel, then "zipped" into a single response.  This is useful
 * when you want to perform operations on a single initial observable, then combine the result into a
 * single observable to continue the sequence

 */
public class Concurrent<T, R> implements Func1<T, Observable<R>> {



    private final Func1<T, R>[] concurrent;
    private final Scheduler scheduler;

    private Concurrent(final Scheduler scheduler, final Func1<T, R>[] concurrent ){
        this.scheduler = scheduler;
        this.concurrent = concurrent;
    }

    @Override
      public Observable<R> call( final T input ) {

        List<Observable<R>> observables = new ArrayList<Observable<R>>();

        //put all our observables together for concurrency
        for( Func1<T, R> funct: concurrent){
            final Observable<R> observable = Observable.from(input).subscribeOn(scheduler).map( funct );

            observables.add( observable );
        }



        //TODO not sure why FuncN takes object instead of T can we do this with type safety?
        return Observable.zip(observables, new FuncN<R>() {

            @Override
            public R call( final Object... objects ) {
//                for(Object result: objects){
//                    Preconditions.checkNotNull( result, "A concurrent operation returned null.  This is not expected" );
//                }

                //we don't care which one, since they all should emit the same value
                return (R)objects[0];
            }
        });

      }


    /**
     * Create an instance of concurrent execution.  All functions specified in the list are invoked in parallel.
     * The results are then "zipped" into a single observable which is returned
     *
     *
     * @param scheduler The scheduler to be used to schedule the concurrent tasks
     * @param observable The observable we're invoking on
     * @param concurrent The concurrent operations we're invoking
     * @return
     */
    public static <T, R> Observable<R> concurrent( final Scheduler scheduler, final Observable<T> observable, final Func1<T, R>... concurrent ){
          return observable.mapMany(new Concurrent<T, R>(scheduler,  concurrent ));
    }


}
