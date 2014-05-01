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


import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


/**
 * A stack of clusters to be tested by Judo Chop.
 */
@JsonDeserialize( as = BasicStack.class )
public interface Stack extends Serializable {

    /**
     * Gets a human legible name for this Stack.
     *
     * @return the human readable name
     */
    @JsonProperty
    String getName();

    /**
     * Gets a unique identifier for this Stack.
     *
     * @return a unique identifier as a UUID
     */
    @JsonProperty
    UUID getId();

    /**
     * The IP access rules for inbound and outbound traffic: in AWS this corresponds
     * to a security group.
     *
     * @return the inbound and outbound IP traffic rules
     */
    @JsonProperty
    IpRuleSet getIpRuleSet();

    /**
     * Gets the data center where the instances will be created. In AWS this
     * corresponds to a region and an availability zone combination.
     *
     * @return the data center where instances are created
     */
    @JsonProperty
    String getDataCenter();

    /**
     * Gets a list of Clusters associated with this Stack where the list order
     * reflects Cluster creation order.
     *
     * @return list of Clusters in order of creation
     */
    @JsonProperty
    List<? extends Cluster> getClusters();
}
