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

package org.apache.usergrid.persistence.qakka.core.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.persistence.qakka.QakkaFig;
import org.apache.usergrid.persistence.qakka.distributed.actors.QueueRefresher;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;


@Singleton
public class InMemoryQueue {
    private static final Logger logger = LoggerFactory.getLogger( QueueRefresher.class );

    private final Map<String, Queue<DatabaseQueueMessage>> queuesByName;
    private final Map<String, UUID> newestByQueueName;


    @Inject
    InMemoryQueue(QakkaFig qakkaFig) {
        queuesByName = new HashMap<>( qakkaFig.getQueueInMemorySize() );
        newestByQueueName = new HashMap<>( qakkaFig.getQueueInMemorySize() );
    }

    private Queue<DatabaseQueueMessage> getQueue( String queueName ) {
        synchronized ( queuesByName ) {
            if ( !queuesByName.containsKey( queueName )) {
                queuesByName.put( queueName, new ConcurrentLinkedQueue<>() );
            }
            return queuesByName.get( queueName );
        }
    }

    public void add( String queueName, DatabaseQueueMessage databaseQueueMessage ) {
        UUID newest = newestByQueueName.get( queueName );
        if ( newest == null ) {
            newest = databaseQueueMessage.getQueueMessageId();
        } else {
            if ( databaseQueueMessage.getQueueMessageId().compareTo( newest ) > 0 ) {
                newest = databaseQueueMessage.getQueueMessageId();
            }
        }
        newestByQueueName.put( queueName, newest );
        getQueue( queueName ).add( databaseQueueMessage );
    }

    public UUID getNewest( String queueName ) {
        return newestByQueueName.get( queueName );
    }

    public DatabaseQueueMessage poll( String queueName ) {
        return getQueue( queueName ).poll();
    }

    public int size( String queueName ) {
        return getQueue( queueName ).size();
    }
}
