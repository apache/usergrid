/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.index.impl;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.usergrid.persistence.core.future.BetterFuture;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.IndexOperationMessage;

import com.google.inject.Inject;
import com.google.inject.Singleton;


@Singleton
public class BufferQueueInMemoryImpl implements BufferQueue {


    private final IndexFig fig;
    private final ArrayBlockingQueue<IndexOperationMessage> messages;


    @Inject
    public BufferQueueInMemoryImpl( final IndexFig fig ) {
        this.fig = fig;
        messages = new ArrayBlockingQueue<>( fig.getIndexQueueSize() );
    }


    @Override
    public void offer( final IndexOperationMessage operation ) {
        try {
            messages.offer( operation, fig.getQueueOfferTimeout(), TimeUnit.MILLISECONDS );
        }
        catch ( InterruptedException e ) {
            throw new RuntimeException("Unable to offer message to queue", e);
        }
    }


    @Override
    public List<IndexOperationMessage> take( final int takeSize, final long timeout, final TimeUnit timeUnit ) {

        final List<IndexOperationMessage> response = new ArrayList<>( takeSize );
        try {


            messages.drainTo( response, takeSize );

            //we got something, go process it
            if ( response.size() > 0 ) {
                return response;
            }


            final IndexOperationMessage polled = messages.poll( timeout, timeUnit );

            if ( polled != null ) {
                response.add( polled );

                //try to add more
                messages.drainTo( response, takeSize - 1 );
            }
        }
        catch ( InterruptedException e ) {
            //swallow
        }


        return response;
    }


    @Override
    public void ack( final List<IndexOperationMessage> messages ) {
        //if we have a future ack it
        for ( final IndexOperationMessage op : messages ) {
            op.done();
        }
    }


    @Override
    public void fail( final List<IndexOperationMessage> messages, final Throwable t ) {


        for ( final IndexOperationMessage op : messages ) {
            final BetterFuture<IndexOperationMessage> future = op.getFuture();

            if ( future != null ) {
                future.setError( t );
            }
        }
    }
}
