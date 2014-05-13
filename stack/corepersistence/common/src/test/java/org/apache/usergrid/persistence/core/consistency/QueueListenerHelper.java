/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */
package org.apache.usergrid.persistence.core.consistency;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * A utility class to be used when writing tests that require blocking until asynchronous processing
 * has occurred.
 */
public class QueueListenerHelper<T> {

    private final CountDownLatch successLatch;
    private final CountDownLatch exceptionLatch;
    private List<AsynchronousMessage<T>> successResults;
    private List<ErrorResult<T>> errorResults;


    /**
     * Create a new queueListenerHelper for the specified processor.
     * @param processor The processor to wait for events
     * @param expectedSuccess The count of expected success events
     * @param expectedException The count of expected exception events.
     */
    public QueueListenerHelper(final AsyncProcessor<T> processor, final int expectedSuccess, final int expectedException){

        successLatch =  new CountDownLatch( expectedSuccess );
        exceptionLatch = new CountDownLatch( expectedException );

        successResults = new ArrayList<>(expectedSuccess);
        errorResults = new ArrayList<>(expectedException);

        processor.addCompleteListener( new CompleteListener<T>() {
            @Override
            public void onComplete( final AsynchronousMessage<T> event ) {
                successResults.add( event );
                successLatch.countDown();
            }
        } );

        processor.addErrorListener( new ErrorListener<T>() {
            @Override
            public void onError( final AsynchronousMessage<T> event, final Throwable t ) {
                errorResults.add( new ErrorResult<T>( event, t ) );
                exceptionLatch.countDown();
            }
        } );


    }

    /**
     * Get the success results
     * @return
     */
    public List<AsynchronousMessage<T>> getSuccessResults(){
        return Collections.unmodifiableList( successResults );
    }


    /**
     * Get the error results
     * @return
     */
    public List<ErrorResult<T>>  getErrorResults(){
        return Collections.unmodifiableList( errorResults );
    }


    /**
     * Await both the success and error counts for the specified timeout.  If errors occurs, it is captured and no exception is thrown
     * @param time
     * @param timeUnit
     */
    public void awaitWithErrors(long time, final TimeUnit timeUnit) throws InterruptedException {
        successLatch.await( time, timeUnit );
        exceptionLatch.await( time, timeUnit );
    }


    /**
     * Await both the success and error counts for the specified timeout.  If errors occurs, it is captured and the first exception is thrown9
     * @param time
     * @param timeUnit
     */
    public void awaitWithoutErrors(long time, final TimeUnit timeUnit) throws Throwable {
         awaitWithErrors( time, timeUnit );

        if(errorResults.size() > 0){
            throw errorResults.get( 0 ).getT();
        }
    }


    /**
     * A wrapper to hold the asynchronous result and the throwable
     * @param <T>
     */
    public class ErrorResult<T>{
        private final AsynchronousMessage<T> result;
        private final Throwable t;


        public ErrorResult( final AsynchronousMessage<T> result, final Throwable t ) {
            this.result = result;
            this.t = t;
        }


        public AsynchronousMessage<T> getResult() {
            return result;
        }


        public Throwable getT() {
            return t;
        }
    }



}
