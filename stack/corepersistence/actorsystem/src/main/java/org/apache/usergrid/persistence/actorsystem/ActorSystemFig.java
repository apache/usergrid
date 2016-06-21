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

    String AKKA_UNIQUEVALUE_ACTORS = "collection.akka.uniquevalue.actors";

    String AKKA_UNIQUEVALUE_CACHE_TTL = "collection.akka.uniquevalue.cache.ttl";

    String AKKA_UNIQUEVALUE_RESERVATION_TTL= "collection.akka.uniquevalue.reservation.ttl";

    String AKKA_AUTHORITATIVE_REGION = "collection.akka.uniquevalue.authoritative.region";

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
     * Number of UniqueValueActors to be started on each node
     */
    @Key(AKKA_UNIQUEVALUE_ACTORS)
    @Default("300")
    int getUniqueValueActors();

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
     * Unique Value cache TTL in seconds.
     */
    @Key(AKKA_UNIQUEVALUE_CACHE_TTL)
    @Default("10")
    int getUniqueValueCacheTtl();

    /**
     * Unique Value Reservation TTL in seconds.
     */
    @Key(AKKA_UNIQUEVALUE_RESERVATION_TTL)
    @Default("10")
    int getUniqueValueReservationTtl();
}
