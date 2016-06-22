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

    String AKKA_ENABLED = "collection.akka.enabled";

    String AKKA_HOSTNAME = "collection.akka.hostname";

    String AKKA_REGION = "collection.akka.region";

    String AKKA_REGION_LIST = "usergrid.queue.regionList"; // same region list used by queues

    String AKKA_REGION_SEEDS = "collection.akka.region.seeds";

    String AKKA_AUTHORITATIVE_REGION = "collection.akka.authoritative.region";

    String AKKA_INSTANCES_PER_NODE = "collection.akka.instances-per-node";


    /**
     * Use Akka or nah
     */
    @Key(AKKA_ENABLED)
    @Default("true")
    boolean getAkkaEnabled();

    /**
     * Hostname to be used in Akka configuration.
     */
    @Key(AKKA_HOSTNAME)
    String getHostname();

    /**
     * Local region to be used in Akka configuration.
     */
    @Key(AKKA_REGION)
    String getRegion();

    /**
     * Comma separated list of regions known to cluster.
     */
    @Key(AKKA_REGION_LIST)
    String getRegionList();

    /**
     * Comma-separated lists of seeds each with format {region}:{hostname}:{port}.
     * Regions MUST be listed in the 'usergrid.queue.regionList'
     */
    @Key(AKKA_REGION_SEEDS)
    String getRegionSeeds();

    /**
     * If no region specified for type, use the authoritative region
     */
    @Key(AKKA_AUTHORITATIVE_REGION)
    String getAkkaAuthoritativeRegion();


    /**
     * Number of actor instances to create on each node for each router.
     */
    @Key(AKKA_INSTANCES_PER_NODE)
    @Default("300")
    int getInstancesPerNode();
}
