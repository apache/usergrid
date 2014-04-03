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


import java.util.Iterator;

import org.apache.usergrid.persistence.graph.GraphFig;

import rx.Scheduler;
import rx.functions.Action1;


/**
 *
 *
 */
public class TimeoutTask<T> implements Action1<Scheduler.Inner> {

    private final AsyncProcessor<T> processor;
    private final GraphFig graphFig;


    public TimeoutTask( final AsyncProcessor<T> processor, final GraphFig graphFig ) {
        this.processor = processor;
        this.graphFig = graphFig;
    }


    @Override
    public void call( final Scheduler.Inner inner ) {

        /**
         * We purposefully loop through a tight loop.  If we have anything to process, we need to do so
         * Once we run out of items to process, this thread will sleep and the timer will fire
         */
        while(!inner.isUnsubscribed() && graphFig.getTimeoutReadSize() > 0) {

            Iterator<AsynchronousMessage<T>> timeouts = getTimeouts();

            /**
             * We're done, just exit
             */
            if(!timeouts.hasNext()){
                return;
            }

            while ( timeouts.hasNext() ) {
                processor.start( timeouts.next() );
            }

        }
    }


    /**
     * Get the timeouts
     * @return
     */
    private Iterator<AsynchronousMessage<T>> getTimeouts() {
        return processor.getTimeouts( graphFig.getTimeoutReadSize(), graphFig.getRepairTimeout() * 2 ).iterator();
    }
}
