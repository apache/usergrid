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


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


/**
 * Represents a group of instances working together.
 */
@JsonDeserialize( as = BasicCluster.class )
public interface Cluster {

    /**
     * The name of the cluster.
     *
     * @return the name of the cluster
     */
    @JsonProperty
    String getName();

    /**
     * The instance specification to use for creating cluster instances.
     *
     * @return the instance specification for cluster instances
     */
    @JsonProperty
    InstanceSpec getInstanceSpec();

    /**
     * The number of instances to use for the cluster.
     *
     * @return the number of cluster instances
     */
    @JsonProperty
    int getSize();
}
