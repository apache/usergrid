/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.stack;


import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Preconditions;


/**
 * A Cluster that also tracks instances and is as a data structure by the coordinator.
 *
 * @todo can ask about all instance states via this class
 * @todo should be able to ask if the cluster is ready to start the test
 */
public class CoordinatedCluster implements ICoordinatedCluster {

    private final Cluster delegate;
    private final Set<Instance> instances;


    CoordinatedCluster( Cluster cluster ) {
        instances = new HashSet<Instance>( cluster.getSize() );
        delegate = cluster;
    }


    @Override
    public String getName() {
        return delegate.getName();
    }


    @Override
    public InstanceSpec getInstanceSpec() {
        return delegate.getInstanceSpec();
    }


    @Override
    public int getSize() {
        return delegate.getSize();
    }


    public Collection<Instance> getInstances() {
        return instances;
    }


    @Override
    public boolean add( Instance instance ) {
        Preconditions.checkState( instances.size() < delegate.getSize(), "Cannot add instances to " +
            delegate.getName() + " cluster: already at maximum size of " + delegate.getSize() );

        return instances.add( instance );
    }
}
