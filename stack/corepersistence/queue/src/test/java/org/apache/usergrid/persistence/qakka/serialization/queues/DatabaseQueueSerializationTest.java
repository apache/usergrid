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

package org.apache.usergrid.persistence.qakka.serialization.queues;

import org.apache.usergrid.persistence.qakka.core.CassandraClientImpl;
import org.apache.usergrid.persistence.qakka.AbstractTest;
import org.apache.usergrid.persistence.qakka.core.CassandraClient;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by russo on 6/9/16.
 */
public class DatabaseQueueSerializationTest extends AbstractTest {

    @Test
    public void writeQueue(){

        CassandraClient cassandraClient = getInjector().getInstance( CassandraClientImpl.class );
        QueueSerialization queueSerialization = getInjector().getInstance( QueueSerialization.class );

        DatabaseQueue queue = new DatabaseQueue("test", "west", "west", 0L, 0, 0, "test_dlq");

        queueSerialization.writeQueue(queue);

        queueSerialization.deleteQueue( queue.getName() );

    }

    @Test
    public void loadQueue(){

        CassandraClient cassandraClient = getInjector().getInstance( CassandraClientImpl.class );
        QueueSerialization queueSerialization = getInjector().getInstance( QueueSerialization.class );

        DatabaseQueue queue = new DatabaseQueue("test1", "west", "west", 0L, 0, 0, "test_dlq");

        queueSerialization.writeQueue(queue);
        DatabaseQueue returnedQueue = queueSerialization.getQueue("test1");

        assertEquals(queue, returnedQueue);

        queueSerialization.deleteQueue( queue.getName() );
    }

    @Test
    public void deleteQueue(){

        CassandraClient cassandraClient = getInjector().getInstance( CassandraClientImpl.class );
        QueueSerialization queueSerialization = getInjector().getInstance( QueueSerialization.class );

        DatabaseQueue queue = new DatabaseQueue("test1", "west", "west", 0L, 0, 0, "test_dlq");

        queueSerialization.writeQueue(queue);
        DatabaseQueue returnedQueue = queueSerialization.getQueue("test1");

        assertEquals(queue, returnedQueue);

        queueSerialization.deleteQueue(queue.getName());

        assertNull(queueSerialization.getQueue("test1"));

    }


}
