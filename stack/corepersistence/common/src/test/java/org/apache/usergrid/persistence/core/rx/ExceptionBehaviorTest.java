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


import org.junit.Test;

import rx.Observable;
import rx.Observer;


/**
 * Tests RX exception behavior
 */
public class ExceptionBehaviorTest {

    //this test shows toBlocking re-throws exceptions correctly
    @Test( expected = TestException.class )
    public void throwOnBlocking() {

        Observable.range( 0, 1 ).map( integer -> {
            throw new TestException( "I throw and exception" );
        } ).toBlocking().first();
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
