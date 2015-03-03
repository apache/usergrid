/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */
package org.apache.usergrid.persistence.index.impl;

import com.codahale.metrics.Counter;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.persistence.core.future.BetterFuture;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.index.IndexBufferProducer;
import org.apache.usergrid.persistence.index.IndexOperationMessage;
import rx.Subscriber;

/**
 * Producer for index operation messages
 */
@Singleton
public class EsIndexBufferProducerImpl implements IndexBufferProducer {

    private final Counter indexSizeCounter;
    private Subscriber<? super IndexOperationMessage> subscriber;

    @Inject
    public EsIndexBufferProducerImpl(MetricsFactory metricsFactory){
        this.indexSizeCounter = metricsFactory.getCounter(EsIndexBufferProducerImpl.class,"index.buffer.size");

    }
    @Override
    public void call(Subscriber<? super IndexOperationMessage> subscriber) {
        this.subscriber = subscriber;
    }

    public BetterFuture put(IndexOperationMessage message){
        Preconditions.checkNotNull(message,"Message cannot be null");
        indexSizeCounter.inc(message.getOperations().size());
        subscriber.onNext(message);
        return message.getFuture();
    }
}
