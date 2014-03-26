package org.apache.usergrid.persistence.graph.serialization;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

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


    @Test
    public void mergeTest(){

        final int sizePerObservable = 2000;

        Observable<Integer> input1 = getObservables( sizePerObservable );
        Observable<Integer> input2 = getObservables( sizePerObservable );

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


        assertEquals("Count was correct", sizePerObservable*2, returned);
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

                    //Sleep for a very long time before emitting the last value
                    if(i == size -1){
                        try {
                            Thread.sleep(5000);
                        }
                        catch ( InterruptedException e ) {
                            subscriber.onError( e );
                            return;
                        }
                    }


                    subscriber.onNext( values.get( i ) );
                }

                subscriber.onCompleted();

                //purposefully no error handling here
            }
        } ).subscribeOn( Schedulers.io() );

    }
}
