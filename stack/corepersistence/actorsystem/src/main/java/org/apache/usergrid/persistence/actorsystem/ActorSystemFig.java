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
package org.apache.usergrid.persistence.actorsystem;


import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;

import java.io.Serializable;


@FigSingleton
public interface ActorSystemFig extends GuicyFig, Serializable {

    String CLUSTER_ENABLED = "usergrid.cluster.enabled";

    String CLUSTER_REGIONS_LIST = "usergrid.cluster.region.list";

    String CLUSTER_REGIONS_LOCAL = "usergrid.cluster.region.local";

    String CLUSTER_SEEDS = "usergrid.cluster.seeds";

    String CLUSTER_PORT = "usergrid.cluster.port";

    String CLUSTER_IO_EXECUTOR_TYPE = "usergrid.cluster.io.executor";

    String CLUSTER_IO_EXECUTOR_THREAD_POOL_SIZE = "usergrid.cluster.io.thread-pool-size";

    String CLUSTER_IO_EXECUTOR_REJECTION_POLICY = "usergrid.cluster.io.rejection-policy";





    /**
     * Use Cluster or nah
     */
    @Key(CLUSTER_ENABLED)
    @Default("true")
    boolean getEnabled();

    /**
     * Local region to be used in Akka configuration.
     */
    @Key(CLUSTER_REGIONS_LOCAL)
    @Default("default")
    String getRegionLocal();

    /**
     * Comma separated list of regions known to cluster.
     */
    @Key(CLUSTER_REGIONS_LIST)
    @Default("default")
    String getRegionsList();

    /**
     * Comma-separated lists of seeds each with format {region}:{hostname}
     */
    @Key(CLUSTER_SEEDS)
    @Default("default:localhost")
    String getSeeds();

    /**
     * Port for cluster comms.
     */
    @Key(CLUSTER_PORT)
    @Default("2551")
    String getPort();

    /**
     *  Hostname used for advertising to the cluster what itself should be reference as
     */
    @Key("usergrid.cluster.hostname")
    @Default("")
    String getHostname();

    /**
     *  Possible executor types for any blocking IO actors in the actor system.
     */
    @Key(CLUSTER_IO_EXECUTOR_TYPE)
    @Default("thread-pool-executor")
    String getClusterIoExecutorType();

    /**
     *  Number of threads to be used when using the fixed thread pool size in the blocking IO executor
     *  Not relevant if anything other than "thread-pool-executor" is configured.
     */
    @Key(CLUSTER_IO_EXECUTOR_THREAD_POOL_SIZE)
    @Default("25")
    int getClusterIoExecutorThreadPoolSize();

    /** Only used with "thread-pool-executor" and the following values are valid:
     *
     *  abort-policy
     *  caller-runs-policy
     *  discard-oldest-policy
     *  discard-policy
     *
     *  Not relevant if anything other than "thread-pool-executor" is configured.
     */
    @Key(CLUSTER_IO_EXECUTOR_REJECTION_POLICY)
    @Default("caller-runs-policy")
    String getClusterIoExecutorRejectionPolicy();
}
