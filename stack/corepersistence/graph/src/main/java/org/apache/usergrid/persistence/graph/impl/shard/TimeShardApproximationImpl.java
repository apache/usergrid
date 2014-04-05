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

package org.apache.usergrid.persistence.graph.impl.shard;


import java.nio.charset.Charset;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.clearspring.analytics.hash.MurmurHash;
import com.clearspring.analytics.stream.cardinality.HyperLogLog;


/**
 * Implementation for doing approximation.  Uses hy perlog log.
 *
 *
 * http://blog.aggregateknowledge.com/2012/10/25/sketch-of-the-day-hyperloglog-cornerstone-of-a-big-data-infrastructure/
 *
 * See also
 *
 * http://blog.aggregateknowledge.com/2012/10/25/sketch-of-the-day-hyperloglog-cornerstone-of-a-big-data-infrastructure/
 *
 * See also
 *
 * https://github.com/addthis/stream-lib/blob/master/src/main/java/com/clearspring/analytics/stream/cardinality
 * /HyperLogLog.java
 */


public class TimeShardApproximationImpl implements TimeShardApproximation {

    //TODO T.N. refactor into an expiring local cache.  We need each hyperlog to be it's own instance of a given shard
    //if this is to work
    private static final String UTF_8 = "UTF-8";
    private static final Charset CHARSET = Charset.forName( UTF_8 );

    /**
     * We generate a new time uuid every time a new instance is started.  We should never re-use an instance.
     */
    private final UUID identity = UUIDGenerator.newTimeUUID();
    private final HyperLogLog hyperLogLog;


    /**
     * Create a time shard approximation with the correct configuration.
     */
    @Inject
    public TimeShardApproximationImpl( final GraphFig graphFig ) {
        hyperLogLog = new HyperLogLog( graphFig.getCounterPrecisionLoss() );
    }


    @Override
    public void increment( final OrganizationScope scope, final Id nodeId, final UUID shardId,
                           final String... edgeType ) {

        byte[] hash = hash( scope, nodeId, shardId, edgeType );


        long longHash = MurmurHash.hash64( hash, hash.length );


        hyperLogLog.offerHashed( longHash );
    }


    @Override
    public long getCount( final OrganizationScope scope, final Id nodeId, final UUID shardId,
                          final String... edgeType ) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }


    private byte[] hash( final OrganizationScope scope, final Id nodeId, final UUID shardId,
                         final String... edgeTypes ) {
        StringBuilder builder = new StringBuilder();

        final Id organization = scope.getOrganization();

        builder.append( organization.getUuid() );
        builder.append( organization.getType() );

        builder.append( nodeId.getUuid() );
        builder.append( nodeId.getType() );

        builder.append( shardId.toString() );

        for ( String edgeType : edgeTypes ) {
            builder.append( edgeType );
        }

        return builder.toString().getBytes( CHARSET );
    }
}
