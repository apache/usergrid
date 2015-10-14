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


import org.apache.usergrid.persistence.index.impl.IndexProducer;
import org.junit.Rule;
import org.junit.runner.RunWith;

import org.apache.usergrid.corepersistence.TestIndexModule;
import org.apache.usergrid.corepersistence.asyncevents.AsyncEventService;
import org.apache.usergrid.corepersistence.asyncevents.EventBuilder;
import org.apache.usergrid.corepersistence.asyncevents.InMemoryAsyncEventService;
import org.apache.usergrid.persistence.core.aws.NoAWSCredsRule;
import org.apache.usergrid.persistence.core.rx.RxTaskScheduler;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.index.impl.EsRunner;

import com.google.inject.Inject;

import net.jcip.annotations.NotThreadSafe;


@RunWith( EsRunner.class )
@UseModules( { TestIndexModule.class } )
@NotThreadSafe
public class InMemoryAsycIndexServiceTest extends AsyncIndexServiceTest {

    @Rule
    public NoAWSCredsRule noAwsCredsRule = new NoAWSCredsRule();


    @Inject
    public EventBuilder eventBuilder;

    @Inject
    public RxTaskScheduler rxTaskScheduler;


    @Inject
    public IndexProducer indexProducer;
    @Override
    protected AsyncEventService getAsyncEventService() {
        return  new InMemoryAsyncEventService( eventBuilder, rxTaskScheduler,indexProducer, false  );
    }





}
