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

package org.apache.usergrid.persistence.qakka.distributed.actors;

import akka.actor.UntypedActor;
import com.google.inject.Inject;
import org.apache.usergrid.persistence.qakka.distributed.messages.QueueRefreshRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class QueueRefresher extends UntypedActor {
    private static final Logger logger = LoggerFactory.getLogger( QueueRefresher.class );

    private final QueueActorHelper queueActorHelper;


    @Inject
    public QueueRefresher( QueueActorHelper queueActorHelper ) {
        this.queueActorHelper = queueActorHelper;
    }


    @Override
    public void onReceive(Object message) {

        if ( message instanceof QueueRefreshRequest ) {

            QueueRefreshRequest request = (QueueRefreshRequest) message;
            String queueName = request.getQueueName();
            queueRefresh( queueName );

        } else {
            unhandled( message );
        }
    }


    void queueRefresh( String queueName ) {
        queueActorHelper.queueRefresh( queueName );
    }
}
