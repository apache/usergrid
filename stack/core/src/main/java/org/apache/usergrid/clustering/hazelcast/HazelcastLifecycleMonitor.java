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
package org.apache.usergrid.clustering.hazelcast;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.Instance;
import com.hazelcast.core.InstanceEvent;
import com.hazelcast.core.InstanceListener;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;


public class HazelcastLifecycleMonitor implements InstanceListener, MembershipListener {

    private static final Logger logger = LoggerFactory.getLogger( HazelcastLifecycleMonitor.class );


    public HazelcastLifecycleMonitor() {
    }


    public void init() {
        logger.info( "HazelcastLifecycleMonitor initializing..." );
        Hazelcast.addInstanceListener( this );
        Hazelcast.getCluster().addMembershipListener( this );
        logger.info( "HazelcastLifecycleMonitor initialized" );
    }


    public void destroy() {
        logger.info( "Shutting down Hazelcast" );
        Hazelcast.shutdownAll();
        logger.info( "Hazelcast shutdown" );
    }


    @Override
    public void instanceCreated( InstanceEvent event ) {
        Instance instance = event.getInstance();
        logger.info( "Created instance ID: [" + instance.getId() + "] Type: [" + instance.getInstanceType() + "]" );
    }


    @Override
    public void instanceDestroyed( InstanceEvent event ) {
        Instance instance = event.getInstance();
        logger.info( "Destroyed isntance ID: [" + instance.getId() + "] Type: [" + instance.getInstanceType() + "]" );
    }


    @Override
    public void memberAdded( MembershipEvent membersipEvent ) {
        logger.info( "MemberAdded " + membersipEvent );
    }


    @Override
    public void memberRemoved( MembershipEvent membersipEvent ) {
        logger.info( "MemberRemoved " + membersipEvent );
    }
}
