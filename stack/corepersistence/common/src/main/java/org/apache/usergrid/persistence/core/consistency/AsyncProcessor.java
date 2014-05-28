/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.core.consistency;


import java.util.Collection;


/**
 * Used to fork lazy repair and other types of operations.
 */
public interface AsyncProcessor<T> {

    /**
     * Start the consumption of the timeout events
     */
    public void start();

    /**
     * Stop consumption
     */
    public void stop();

    /**
     * The processor implementation is responsible for guaranteeing the events fire in the runtime environment. This
     * could be local or clustered, consult the documentation on the implementation.  Note that events published here
     * could possibly be double published if the operation reaches it's timeout before completion.  As a result, every
     * receiver of the event should operate in an idempotent way.  Note that the event will fire at a time >= the
     * timeout time. Firing immediately should not be assumed.
     *
     * @param event The event to be scheduled for verification
     * @param timeout The time in milliseconds we should wait before the event should fire
     */
    public AsynchronousMessage<T> setVerification( T event, long timeout );


    /**
     * Start processing the event immediately asynchronously.  In the event an exception is thrown, the
     * AsynchronousMessage should be re-tried. It is up to the implementer to commit the event so that it does not fire
     * again.  This should never throw exceptions.
     *
     * @param event The event to start
     */
    public void start( AsynchronousMessage<T> event );


    /**
     * Get all events that have passed their timeout
     *
     * @param maxCount The maximum count
     * @param timeout The timeout to set when retrieving these timeouts to ensure they aren't lost
     *
     * @return A collection of asynchronous messages that have passed their timeout.  This could be due to process
     *         failure node loss etc.  No assumptions regarding the state of the message should be assumed when they are
     *         returned.
     */
    public Collection<AsynchronousMessage<T>> getTimeouts( int maxCount, long timeout );

    /**
     * Add the error listener to the list of listeners
     */
    public void addErrorListener( ErrorListener<T> listener );

    /**
     * Add the listener to this instance
     */
    public <R> void  addListener( MessageListener<T, R> listener );

    /**
     * Add a complete listener that is invoked when the listener has been invoked
     */
    public void addCompleteListener( CompleteListener<T> listener );
}
