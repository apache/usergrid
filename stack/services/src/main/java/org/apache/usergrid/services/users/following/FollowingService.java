/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.services.users.following;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.apache.usergrid.persistence.ConnectionRef;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.services.AbstractConnectionsService;


public class FollowingService extends AbstractConnectionsService {

    private static final Logger logger = LoggerFactory.getLogger( FollowingService.class );


    @Override
    public ConnectionRef createConnection( EntityRef connectingEntity, String connectionType,
                                           EntityRef connectedEntityRef ) throws Exception {
        copyActivityFeed( connectingEntity, connectedEntityRef );
        return em.createConnection( connectingEntity, connectionType, connectedEntityRef );
    }


    @Override
    public void deleteConnection( ConnectionRef connectionRef ) throws Exception {
        em.deleteConnection( connectionRef );
    }


    public void copyActivityFeed( final EntityRef connectingEntity, final EntityRef connectedEntityRef )
            throws Exception {
        if (logger.isTraceEnabled()) {
            logger.trace("Copying activities to feed...");
        }
        TaskExecutor taskExecutor = ( TaskExecutor ) getApplicationContext().getBean( "taskExecutor" );
        taskExecutor.execute( new Runnable() {
            @Override
            public void run() {
                try {
                    em.copyRelationships( connectedEntityRef, "activities", connectingEntity, "feed" );
                }
                catch ( Exception e ) {
                    logger.error( "Error while copying activities into feed", e );
                }
            }
        } );
    }
}
