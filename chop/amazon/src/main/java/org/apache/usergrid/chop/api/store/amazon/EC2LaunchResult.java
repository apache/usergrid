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
package org.apache.usergrid.chop.api.store.amazon;


import java.util.Collection;

import org.apache.usergrid.chop.spi.LaunchResult;
import org.apache.usergrid.chop.stack.Instance;
import org.apache.usergrid.chop.stack.InstanceSpec;


public class EC2LaunchResult implements LaunchResult {

    private InstanceSpec instanceSpec;
    private Collection<Instance> instances;


    public EC2LaunchResult( InstanceSpec spec, Collection<Instance> instances ) {
        this.instanceSpec = spec;
        this.instances = instances;
    }

    @Override
    public int getCount() {
        return instances.size();
    }


    @Override
    public InstanceSpec getInstanceSpec() {
        return instanceSpec;
    }


    @Override
    public Iterable<Instance> getInstances() {
        return instances;
    }
}
