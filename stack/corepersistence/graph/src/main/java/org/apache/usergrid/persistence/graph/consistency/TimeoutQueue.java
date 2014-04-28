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
package org.apache.usergrid.persistence.graph.consistency;


import java.io.Serializable;
import java.util.Collection;


/**
 * Interface for implementations of a timeout queue.
 */
public interface TimeoutQueue<T extends Serializable> {

    /**
     * Queue the event with the timeout provided
     *
     * @param event The event to queue
     * @param timeout The timeout to set on the queue element before it becomes available for consumption
     * @return The AsynchronousMessage that has been queued
     */
    public AsynchronousMessage<T> queue( T event, long timeout );


    /**
     * Take up to maxSize elements with a timeout <= the currentTime
     *
     * This implicitly re-schedules every taken operation at currentTime+timeout
     *
     * @param  maxSize The maximum number of elements to take
     * @param timeout The timeout to set when taking the elements from the Q and allowing them to become available
     *
     * @return A collection of events.
     */
    public Collection<AsynchronousMessage<T>> take( int maxSize, long timeout );


    /**
     * Remove this timeout event from the queue.
     *
     * @param event The event to remove
     *
     * @return True if the element was removed.  False otherwise
     */
    public boolean remove( AsynchronousMessage<T> event );
}
