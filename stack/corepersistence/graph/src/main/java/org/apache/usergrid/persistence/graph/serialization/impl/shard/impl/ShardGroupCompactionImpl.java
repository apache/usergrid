/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.apache.usergrid.persistence.graph.serialization.impl.shard.impl;


import java.util.Collection;

import org.apache.usergrid.persistence.core.consistency.TimeService;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardEntryGroup;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardGroupCompaction;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;


/**
 * Implementation of the shard group compaction
 */
@Singleton
public class ShardGroupCompactionImpl implements ShardGroupCompaction {


    private final TimeService timeService;


    @Inject
    public ShardGroupCompactionImpl( final TimeService timeService ) {this.timeService = timeService;}


    @Override
    public Observable<Integer> compact( final ShardEntryGroup group ) {
        final long startTime = timeService.getCurrentTime();

        Preconditions.checkNotNull(group, "group cannot be null");
        Preconditions.checkArgument( group.isCompactionPending(), "Compaction is pending" );
        Preconditions.checkArgument( group.shouldCompact(startTime  ), "Compaction can now be run" );


        final Shard targetShard = group.getCompactionTarget();

        final Collection<Shard> sourceShards = group.getReadShards();


        //now get iterators for each of the source shards, and then copy them to the compaction target shard




        return null;

    }
}
