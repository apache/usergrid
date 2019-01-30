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
package org.apache.usergrid.persistence.collection.uniquevalues;

import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;
import java.io.Serializable;


@FigSingleton
public interface UniqueValuesFig extends GuicyFig, Serializable {

    String UNIQUEVALUE_USE_CLUSTER = "collection.uniquevalues.usecluster";
    
    String UNIQUEVALUE_SKIP_REMOTE_REGIONS = "collection.uniquevalues.skip.remote.regions";

    String UNIQUEVALUE_ACTORS = "collection.uniquevalues.actors";

    String UNIQUEVALUE_CACHE_TTL = "collection.uniquevalues.cache.ttl";

    String UNIQUEVALUE_RESERVATION_TTL= "collection.uniquevalues.reservation.ttl";

    String UNIQUEVALUE_AUTHORITATIVE_REGION = "collection.uniquevalues.authoritative.region";

    String UNIQUEVALUE_REQUEST_TIMEOUT = "collection.uniquevalues.request.timeout";

    String UNIQUEVALUE_REQUEST_RETRY_COUNT = "collection.uniquevalues.request.retrycount";
    
    


    /**
     * Tells Usergrid whether or not to use the Akka Cluster system to verify unique values ( more consistent)
     * Setting this to false by default to avoid extra complications by default.
     */
    @Key(UNIQUEVALUE_USE_CLUSTER)
    @Default("false")
    boolean getUnqiueValueViaCluster();
    
    /**
     * Tells Usergrid to restrict UniqueValue related chatter to local Akka Cluster only. Skips remote regions
     * Setting this to true by default to avoid extra complications by default.
     */
    @Key(UNIQUEVALUE_SKIP_REMOTE_REGIONS)
    @Default("true")
    boolean getSkipRemoteRegions();

    /**
     * Unique Value cache TTL in seconds.
     */
    @Key(UNIQUEVALUE_CACHE_TTL)
    @Default("10")
    int getUniqueValueCacheTtl();

    /**
     * Unique Value Reservation TTL in seconds.
     */
    @Key(UNIQUEVALUE_RESERVATION_TTL)
    @Default("10")
    int getUniqueValueReservationTtl();

    /**
     * Number of actor instances to create on each.
     */
    @Key(UNIQUEVALUE_ACTORS)
    @Default("300")
    int getUniqueValueInstancesPerNode();

    /**
     * Primary authoritative region (used if none other specified).
     */
    @Key(UNIQUEVALUE_AUTHORITATIVE_REGION)
    String getAuthoritativeRegion();

    /**
     * Number of milliseconds before timing out the unique value request to the Actor System
     */
    @Key(UNIQUEVALUE_REQUEST_TIMEOUT)
    @Default("5000")
    int getRequestTimeout();

    /**
     * Number of actor instances to create on each.
     */
    @Key(UNIQUEVALUE_REQUEST_RETRY_COUNT)
    @Default("2")
    int getRequestRetryCount();
}
