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
package org.apache.usergrid.persistence.graph.serialization.impl.shard;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;


/**
 * There are cases where we need to read or write to more than 1 shard.  This object encapsulates a set of shards that
 * should be written to and read from.  All reads should combine the data sets from all shards in the group, and writes
 * should be written to each shard.  Once the shard can safely be compacted a background process should be triggered to
 * remove additional shards and make seeks faster.  This multiread/write should only occur during the time period of the
 * delta (in milliseconds), after which the next read will asynchronously compact the shards into a single shard.
 */
public class ShardEntryGroup {

    private static final Logger LOG = LoggerFactory.getLogger( ShardEntryGroup.class );

    private List<Shard> shards;

    private final long delta;

    private long maxCreatedTime;

    private Shard compactionTarget;

    private Shard rootShard;


    /**
     * The max delta we accept in milliseconds for create time to be considered a member of this group
     */
    public ShardEntryGroup( final long delta ) {
        Preconditions.checkArgument( delta > 0, "delta must be greater than 0" );
        this.delta = delta;
        this.shards = new ArrayList<>();
        this.maxCreatedTime = 0;
    }


    /**
     * Only add a shard if it is within the rules require to meet a group.  The rules are outlined below.
     *
     * Case 1)  First shard in the group, always added
     *
     * Case 2) Shard is unmerged, it should be included with it's peers since other nodes may not have it yet
     *
     * Case 3) The list contains only non compacted shards, and this is last and and merged.  It is considered a lower
     * bound
     */
    public boolean addShard( final Shard shard ) {

        Preconditions.checkNotNull( "shard cannot be null", shard );

        final int size = shards.size();

        if ( size == 0 ) {
            addShardInternal( shard );
            return true;
        }

        final Shard minShard = shards.get( size - 1 );

        Preconditions.checkArgument( minShard.compareTo( shard ) > 0, "shard must be less than the current max" );

        //shard is not compacted, or it's predecessor isn't, we should include it in this group
        if ( !minShard.isCompacted() ) {
            addShardInternal( shard );

            return true;
        }




        return false;
    }


    /**
     * Add the shard and set the min created time
     */
    private void addShardInternal( final Shard shard ) {
        shards.add( shard );

        maxCreatedTime = Math.max( maxCreatedTime, shard.getCreatedTime() );

        //we're changing our structure, unset the compaction target
        compactionTarget = null;
    }


    /**
     * Return the minum shard based on time indexes
     */
    public Shard getMinShard() {
        final int size = shards.size();

        if ( size < 1 ) {
            return null;
        }

        return shards.get( size - 1 );
    }


    /**
     * Get the entries that we should read from.
     */
    public Collection<Shard> getReadShards() {


        final Shard staticShard = getRootShard();
        final Shard compactionTarget = getCompactionTarget();



        if(compactionTarget != null){
            LOG.debug( "Returning shards {} and {} as read shards", compactionTarget, staticShard );
            return Arrays.asList( compactionTarget, staticShard );
        }


        LOG.debug( "Returning shards {} read shard", staticShard );
        return  Collections.singleton( staticShard );
    }




    /**
     * Get the entries, with the max shard time being first. We write to all shards until they're migrated
     */
    public Collection<Shard> getWriteShards( long currentTime ) {

        /**
         * The shards in this set can be combined, we should only write to the compaction target to avoid
         * adding data to other shards
         */
        if ( !isTooSmallToCompact() && shouldCompact( currentTime ) ) {

            final Shard compactionTarget = getCompactionTarget();

            LOG.debug( "Returning shard {} as write shard", compactionTarget);

            return Collections.singleton( compactionTarget  );

        }

        final Shard staticShard = getRootShard();


        LOG.debug( "Returning shard {} as write shard", staticShard);

        return Collections.singleton( staticShard );

    }


    /**
     * Return true if we have a pending compaction
     */
    public boolean isCompactionPending() {
        return !isTooSmallToCompact();
    }


    /**
     * Get the root shard that was created in this group
     * @return
     */
    private Shard getRootShard(){
        if(rootShard != null){
            return rootShard;
        }

        final Shard rootCandidate = shards.get( shards.size() -1 );

        if(rootCandidate.isCompacted()){
            rootShard = rootCandidate;
        }

        return rootShard;
    }

    /**
     * Get the shard all compactions should write to.  Null indicates we cannot find a shard that could be used as a
     * compaction target.  Note that this shard may not have surpassed the delta yet You should invoke "shouldCompact"
     * first to ensure all criteria are met before initiating compaction
     */
    public Shard getCompactionTarget() {

        if ( compactionTarget != null ) {
            return compactionTarget;
        }


        //we have < 2 shards, we can't compact
        if ( isTooSmallToCompact() ) {
            return null;
        }


        final int lastIndex = shards.size() - 1;

        final Shard last = shards.get( lastIndex );

        //Our oldest isn't compacted. As a result we have no "bookend" to delimit this entry group.  Therefore we
        // can't compact
        if ( !last.isCompacted() ) {
            return null;
        }

        //Start seeking from the end of our group.  The first shard we encounter that is not compacted is our
        // compaction target
        //NOTE: This does not mean we can compact, rather it's just an indication that we have a target set.
        for ( int i = lastIndex - 1; i > -1; i-- ) {
            final Shard compactionCandidate = shards.get( i );


            if ( !compactionCandidate.isCompacted() ) {
                compactionTarget = compactionCandidate;
                break;
            }
        }

        return compactionTarget;
    }


    /**
     * Return the number of entries in this shard group
     */
    public int entrySize() {
        return shards.size();
    }


    /**
     * Return true if there are not enough elements in this entry group to consider compaction
     */
    private boolean isTooSmallToCompact() {
        return shards.size() < 2;
    }


    /**
     * Returns true if the newest created shard is path the currentTime - delta
     *
     * @param currentTime The current system time in milliseconds
     *
     * @return True if these shards can safely be combined into a single shard, false otherwise
     */
    public boolean shouldCompact( final long currentTime ) {

        /**
         * We don't have enough shards to compact, ignore
         */
        return getCompactionTarget() != null


                /**
                 * If something was created within the delta time frame, not everyone may have seen it due to
                 * cache refresh, we can't compact yet.
                 */

                && currentTime - delta > maxCreatedTime;
    }


    /**
     * Return true if this shard can be deleted AFTER all of the data in it has been moved
     */
    public boolean canBeDeleted( final Shard shard ) {
        //if we're a neighbor shard (n-1) or the target compaction shard, we can't be deleted
        //we purposefully use shard index comparison over .equals here, since 2 shards might have the same index with
        // different timestamps
        // (unlikely but could happen)

        final Shard compactionTarget = getCompactionTarget();


        return !shard.isCompacted() && ( compactionTarget != null && compactionTarget.getShardIndex() != shard
                .getShardIndex() );
    }


    @Override
    public String toString() {
        return "ShardEntryGroup{" +
                "shards=" + shards +
                ", delta=" + delta +
                ", maxCreatedTime=" + maxCreatedTime +
                ", compactionTarget=" + compactionTarget +
                '}';
    }
}
