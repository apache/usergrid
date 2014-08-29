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


/**
 * A basic Cluster implementation.
 */
public class BasicCluster implements Cluster {
    private String name;
    private InstanceSpec instanceSpec = new BasicInstanceSpec();
    private int size;


    @Override
    public String getName() {
        return name;
    }


    public BasicCluster setName( final String name ) {
        this.name = name;
        return this;
    }


    @Override
    public InstanceSpec getInstanceSpec() {
        return instanceSpec;
    }


    public BasicCluster setInstanceSpec( final InstanceSpec instanceSpec ) {
        this.instanceSpec = instanceSpec;
        return this;
    }


    @Override
    public int getSize() {
        return size;
    }


    public BasicCluster setSize( final int size ) {
        this.size = size;
        return this;
    }
}
