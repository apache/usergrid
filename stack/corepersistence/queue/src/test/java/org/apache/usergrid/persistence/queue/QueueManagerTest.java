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

package org.apache.usergrid.persistence.queue;


import org.apache.usergrid.persistence.queue.QueueManagerFactory;
import org.apache.usergrid.persistence.queue.QueueScope;
import org.apache.usergrid.persistence.queue.guice.TestQueueModule;
import org.apache.usergrid.persistence.queue.impl.QueueScopeImpl;
import org.jukito.UseModules;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.cassandra.ITRunner;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.google.inject.Inject;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


@RunWith( ITRunner.class )
@UseModules( { TestQueueModule.class } )
public class QueueManagerTest {


    @Inject
    protected QueueManagerFactory qmf;

    protected QueueScope scope;


    @Before
    public void mockApp() {
        this.scope = new QueueScopeImpl( new SimpleId( "application" ), "testQueue" );
    }


    @Test
    public void createQueue() {
        QueueManager qm = qmf.getQueueManager(scope);
        qm.createQueue();
        Queue queue = qm.getQueue();
    }

}
