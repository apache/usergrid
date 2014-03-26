package org.apache.usergrid.persistence.graph.serialization;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static org.junit.Assert.assertEquals;


/**
 *
 *
 */
public class TestCount {

    private static final Logger log = LoggerFactory.getLogger(TestCount.class);

    @Test
    public void mergeTest(){

        final int sizePerObservable = 2000;

        Observable<Integer> input1 = getObservables( sizePerObservable ).flatMap( new Func1<Integer, Observable<?
                extends Integer>>() {
            @Override
            public Observable<? extends Integer> call( final Integer integer ) {
                return getObservables( 100 );
            }
        } );
        Observable<Integer> input2 = getObservables( sizePerObservable ).flatMap( new Func1<Integer, Observable<?
                extends Integer>>() {
            @Override
            public Observable<? extends Integer> call( final Integer integer ) {
                return getObservables( 100 );
            }
        } );

       int returned =  Observable.merge(input1, input2).buffer( 1000 ).flatMap(
               new Func1<List<Integer>, Observable<Integer>>() {
                   @Override
                   public Observable<Integer> call( final List<Integer> integers ) {

                       //simulates batching a network operation from buffer, then re-emitting the values passed

                       try {
                           Thread.sleep( 100 );
                       }
                       catch ( InterruptedException e ) {
                           throw new RuntimeException( e );
                       }


                       return Observable.from( integers );
                   }
               } ).count().defaultIfEmpty( 0 ).toBlockingObservable().last();


        assertEquals("Count was correct", sizePerObservable*2*100, returned);
    }


    /**
     * Get observables from the sets
     * @param size
     * @return
     */
    private Observable<Integer> getObservables( int size ){

        final List<Integer> values = new ArrayList<Integer>(size);

        for(int i = 0; i <size; i ++ ) {
            values.add( i );
        }


        /**
         * Simulates occasional sleeps while we fetch something over the network
         */
        return Observable.create( new Observable.OnSubscribe<Integer>() {
            @Override
            public void call( final Subscriber<? super Integer> subscriber ) {

                final int size = values.size();

                for(int i = 0; i < size; i ++){



                    if(i%1000 == 0){
                        //simulate network fetch
                        try {
                            Thread.sleep( 250 );
                        }
                        catch ( InterruptedException e ) {
                            subscriber.onError( e );
                            return;
                        }
                    }

                    final Integer value = values.get( i );

                    log.info( "Emitting {}", value  );


                    subscriber.onNext( value );
                }

                subscriber.onCompleted();

                //purposefully no error handling here
            }
        } ).subscribeOn( Schedulers.io() );

    }
}
