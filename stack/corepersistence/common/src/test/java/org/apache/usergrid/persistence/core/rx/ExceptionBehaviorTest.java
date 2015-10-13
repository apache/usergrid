/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.persistence.core.rx;


import org.junit.Assert;
import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Tests RX exception behavior
 */
public class ExceptionBehaviorTest {

    //this test shows toBlocking re-throws exceptions correctly
    @Test( expected = TestException.class )
    public void throwOnBlockingFirst() {

        Observable.range( 0, 1 ).map( integer -> {
            throw new TestException( "I throw and exception" );
        } ).toBlocking().first();
    }

    @Test( expected = TestException.class )
    public void throwOnBlockingLast() {

        Observable.range( 0, 1 ).map( integer -> {
            throw new TestException( "I throw and exception" );
        } ).toBlocking().last();
    }

    @Test()
    public void testSequence(){
        ArrayList listReturn =  Observable.range(0, 1).flatMap(i -> Observable.empty())
            .collect(()->new ArrayList(),(list,i) ->{
                list.add(i);
            }).toBlocking().lastOrDefault(null);

        Assert.assertEquals(listReturn,new ArrayList<Integer>());
    }

    @Test()
    public void testSequence2(){
        ArrayList listReturn =  Observable.range(0, 2).buffer(2).flatMap(i -> Observable.empty())
            .collect(()->new ArrayList(),(list,i) ->{
                list.add(i);
            }).toBlocking().lastOrDefault(null);

        Assert.assertEquals(listReturn,new ArrayList<Integer>());
    }

    @Test()
    public void testSequence3(){
        ArrayList listReturn =  Observable.range(0, 2)
            .collect(()->new ArrayList(),(list,i) ->{
                list.add(i);
            }).toBlocking().first();

        Assert.assertEquals(listReturn, Observable.range(0, 2).toList().toBlocking().last());
    }

    @Test(expected = TestException.class)
    public void testStreamException(){
        List<Integer> listReturn = new ArrayList<Integer>();
        listReturn.add(0);
        listReturn.add(1);
        listReturn.add(2);
        listReturn.stream().map(i->{
            if(i%2 == 0){
                throw new TestException("test");
            }
            return i * 2;
        }).collect(Collectors.toList());
       Assert.fail("test");
    }

//
//    /**
//     * This shows that no re-throw happens on subscribe.  This is as designed, but not as expected
//     */
//    @Test( expected = TestException.class )
//    public void throwOnSubscribe() {
//
//        Observable.range( 0, 1 ).map( integer -> {
//            throw new TestException( "I throw and exception" );
//        } ).subscribe();
//    }


    /**
     *  Tests working with observers
     */
    @Test( expected = TestException.class )
    public void throwOnSubscribeObservable() {

        final ReThrowObserver exceptionObserver = new ReThrowObserver();


        Observable.range( 0, 1 ).map( integer -> {
            throw new TestException( "I throw and exception" );
        } ).subscribe( exceptionObserver );

        exceptionObserver.checkResult();
    }

    /**
     *  Tests working with observers
     */
    @Test( expected = TestException.class )
    public void throwOnSubscribeObservableNewThread() throws Exception {

        final ReThrowObserver exceptionObserver = new ReThrowObserver();

        Observable.range( 0, 1 ).map(integer -> {
            throw new TestException("I throw and exception");
        })
            .doOnError(t -> exceptionObserver.onError(t))
            .subscribeOn(Schedulers.newThread())
            .subscribe(exceptionObserver);

        for(int i =0; i<5; i++) {
            exceptionObserver.checkResult();
            Thread.sleep(200);
        }
    }


    private static final class TestException extends RuntimeException {

        public TestException( final String message ) {
            super( message );
        }
    }


    private static final class ReThrowObserver implements Observer {

        private RuntimeException t;


        @Override
        public void onCompleted() {

        }


        @Override
        public void onError( final Throwable e ) {

            if ( e instanceof RuntimeException ) {
                t = ( RuntimeException ) e;
            }
            else {
                t = new RuntimeException( e );
            }
        }


        @Override
        public void onNext( final Object o ) {

        }


        public void checkResult() {
            if ( t != null ) {
                throw t;
            }
        }
    }


    ;
}
