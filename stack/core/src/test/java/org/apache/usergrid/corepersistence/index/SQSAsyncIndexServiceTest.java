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


import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.corepersistence.TestIndexModule;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.core.aws.NoAWSCredsRule;
import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.rx.RxTaskScheduler;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.index.ApplicationEntityIndex;
import org.apache.usergrid.persistence.index.CandidateResults;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.SearchEdge;
import org.apache.usergrid.persistence.index.SearchTypes;
import org.apache.usergrid.persistence.index.impl.EsRunner;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.persistence.queue.QueueManagerFactory;

import com.google.inject.Inject;

import net.jcip.annotations.NotThreadSafe;

import rx.Observable;
import rx.schedulers.Schedulers;

import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


@RunWith( EsRunner.class )
@UseModules( { TestIndexModule.class } )
@NotThreadSafe
public class SQSAsyncIndexServiceTest extends AsyncIndexServiceTest {



    @Rule
    public NoAWSCredsRule noAwsCredsRule = new NoAWSCredsRule();



    @Inject
    public QueueManagerFactory queueManagerFactory;

    @Inject
    public IndexProcessorFig indexProcessorFig;


    @Inject
    public MetricsFactory metricsFactory;

    @Inject
    public IndexService indexService;

    @Inject
    public RxTaskScheduler rxTaskScheduler;


    @Override
    protected AsyncIndexService getAsyncIndexService() {
        return  new SQSAsyncIndexService( queueManagerFactory, indexProcessorFig, metricsFactory, indexService,
                    entityCollectionManagerFactory, rxTaskScheduler );
    }





}
