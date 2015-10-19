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

package org.apache.usergrid.corepersistence.index;


import org.apache.usergrid.corepersistence.asyncevents.EventBuilder;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.impl.IndexProducer;
import org.apache.usergrid.persistence.queue.QueueFig;
import org.junit.Rule;
import org.junit.runner.RunWith;

import org.apache.usergrid.corepersistence.TestIndexModule;
import org.apache.usergrid.corepersistence.asyncevents.AsyncEventService;
import org.apache.usergrid.corepersistence.asyncevents.AmazonAsyncEventService;
import org.apache.usergrid.persistence.core.aws.NoAWSCredsRule;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.rx.RxTaskScheduler;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.index.impl.EsRunner;
import org.apache.usergrid.persistence.map.MapManagerFactory;
import org.apache.usergrid.persistence.queue.QueueManagerFactory;

import com.google.inject.Inject;

import net.jcip.annotations.NotThreadSafe;

import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


@RunWith( EsRunner.class )
@UseModules( { TestIndexModule.class } )
@NotThreadSafe
public class AmazonAsyncEventServiceTest extends AsyncIndexServiceTest {



    @Rule
    public NoAWSCredsRule noAwsCredsRule = new NoAWSCredsRule();



    @Inject
    public QueueManagerFactory queueManagerFactory;

    @Inject
    public IndexProcessorFig indexProcessorFig;

    @Inject
    public QueueFig queueFig;


    @Inject
    public MetricsFactory metricsFactory;

    @Inject
    public RxTaskScheduler rxTaskScheduler;

    @Inject
    public EventBuilder eventBuilder;

    @Inject
    public IndexProducer indexProducer;

    @Inject
    public IndexLocationStrategyFactory indexLocationStrategyFactory;

    @Inject
    public MapManagerFactory mapManagerFactory;


    @Inject
    public EntityIndexFactory entityIndexFactory;

    @Override
    protected AsyncEventService getAsyncEventService() {
        return  new AmazonAsyncEventService( queueManagerFactory, indexProcessorFig, indexProducer, metricsFactory,  entityCollectionManagerFactory, indexLocationStrategyFactory, entityIndexFactory, eventBuilder, mapManagerFactory, queueFig,  rxTaskScheduler );
    }





}
