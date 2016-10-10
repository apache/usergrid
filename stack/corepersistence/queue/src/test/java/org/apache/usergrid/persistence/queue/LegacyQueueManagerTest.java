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


import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.usergrid.persistence.actorsystem.ActorSystemFig;
import org.apache.usergrid.persistence.qakka.AbstractTest;
import org.apache.usergrid.persistence.qakka.App;
import org.apache.usergrid.persistence.qakka.core.CassandraClient;
import org.apache.usergrid.persistence.qakka.core.CassandraClientImpl;
import org.apache.usergrid.persistence.qakka.distributed.DistributedQueueService;
import org.apache.usergrid.persistence.queue.impl.LegacyQueueScopeImpl;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class LegacyQueueManagerTest extends AbstractTest {

    public static long queueSeed = System.currentTimeMillis();

    // give each test its own injector
    @Override
    protected Injector getInjector() {
        return Guice.createInjector( new TestModule() );
    }


    @Test
    public void send() throws Exception{

        Injector myInjector = getInjector();

        ActorSystemFig actorSystemFig = myInjector.getInstance( ActorSystemFig.class );
        String region = actorSystemFig.getRegionLocal();

        App app = myInjector.getInstance( App.class );
        app.start( "localhost", getNextAkkaPort(), region );

        final LegacyQueueScopeImpl scope =
            new LegacyQueueScopeImpl( "testQueue" + queueSeed++, LegacyQueueScope.RegionImplementation.LOCAL );
        LegacyQueueManagerFactory qmf = myInjector.getInstance( LegacyQueueManagerFactory.class );
        LegacyQueueManager qm = qmf.getQueueManager(scope);

        String value = "bodytest";
        qm.sendMessage(value);

        Thread.sleep(5000);

        List<LegacyQueueMessage> messageList = qm.getMessages(1, String.class);
        assertTrue(messageList.size() >= 1);
        for(LegacyQueueMessage message : messageList){
            assertEquals( value, message.getBody() );
            qm.commitMessage(message);
        }

        messageList = qm.getMessages(1, String.class);
        assertEquals( 0, messageList.size() );

        DistributedQueueService distributedQueueService = myInjector.getInstance( DistributedQueueService.class );
        distributedQueueService.shutdown();

    }

    @Test
    public void sendMore() throws Exception{

        Injector myInjector = getInjector();

        ActorSystemFig actorSystemFig = myInjector.getInstance( ActorSystemFig.class );
        String region = actorSystemFig.getRegionLocal();

        App app = myInjector.getInstance( App.class );
        app.start( "localhost", getNextAkkaPort(), region );

        final LegacyQueueScopeImpl scope =
            new LegacyQueueScopeImpl( "testQueue" + queueSeed++, LegacyQueueScope.RegionImplementation.LOCAL );
        LegacyQueueManagerFactory qmf = myInjector.getInstance( LegacyQueueManagerFactory.class );
        LegacyQueueManager qm = qmf.getQueueManager(scope);

        HashMap<String,String> values = new HashMap<>();
        values.put("test","Test");

        List<Map<String,String>> bodies = new ArrayList<>();
        bodies.add(values);
        qm.sendMessages(bodies);

        Thread.sleep(5000);

        List<LegacyQueueMessage> messageList = qm.getMessages(1, values.getClass());
        assertTrue(messageList.size() >= 1);
        for(LegacyQueueMessage message : messageList){
            assertTrue(message.getBody().equals(values));
        }
        qm.commitMessages(messageList);

        messageList = qm.getMessages(1, values.getClass());
        assertEquals( 0, messageList.size());

        DistributedQueueService distributedQueueService = myInjector.getInstance( DistributedQueueService.class );
        distributedQueueService.shutdown();
    }

    @Test
    public void queueSize() throws Exception{

        Injector myInjector = getInjector();

        ActorSystemFig actorSystemFig = myInjector.getInstance( ActorSystemFig.class );
        String region = actorSystemFig.getRegionLocal();

        App app = myInjector.getInstance( App.class );
        app.start( "localhost", getNextAkkaPort(), region );

        final LegacyQueueScopeImpl scope =
            new LegacyQueueScopeImpl( "testQueue" + queueSeed++, LegacyQueueScope.RegionImplementation.LOCAL );
        LegacyQueueManagerFactory qmf = myInjector.getInstance( LegacyQueueManagerFactory.class );
        LegacyQueueManager qm = qmf.getQueueManager( scope );

        HashMap<String, String> values = new HashMap<>();
        values.put( "test", "Test" );

        List<Map<String, String>> bodies = new ArrayList<>();
        bodies.add( values );
        long initialDepth = qm.getQueueDepth();
        qm.sendMessages( bodies );
        long depth = 0;
        for (int i = 0; i < 10; i++) {
            depth = qm.getQueueDepth();
            if (depth > 0) {
                break;
            }
            Thread.sleep( 1000 );
        }
        assertTrue( depth > 0 );

        List<LegacyQueueMessage> messageList = qm.getMessages( 10, values.getClass() );
        assertTrue( messageList.size() <= 500 );
        for (LegacyQueueMessage message : messageList) {
            assertTrue( message.getBody().equals( values ) );
        }
        if (messageList.size() > 0) {
            qm.commitMessages( messageList );
        }
        for (int i = 0; i < 10; i++) {
            depth = qm.getQueueDepth();
            if (depth == initialDepth) {
                break;
            }
            Thread.sleep( 1000 );
        }
        assertEquals( initialDepth, depth );

        DistributedQueueService distributedQueueService = myInjector.getInstance( DistributedQueueService.class );
        distributedQueueService.shutdown();
    }

}
