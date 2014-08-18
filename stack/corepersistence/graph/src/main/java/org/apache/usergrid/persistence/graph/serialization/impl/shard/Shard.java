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


public class Shard implements Comparable<Shard> {

    private final long shardIndex;
    private final long createdTime;
    private final boolean compacted;


    public Shard( final long shardIndex, final long createdTime, final boolean compacted ) {
        this.shardIndex = shardIndex;
        this.createdTime = createdTime;
        this.compacted = compacted;
    }


    /**
     * Get the long shard index
     */
    public long getShardIndex() {
        return shardIndex;
    }


    /**
     * Get the timestamp in epoch millis this shard was created
     */
    public long getCreatedTime() {
        return createdTime;
    }


    /**
     * Return true if this shard has been compacted
     */
    public boolean isCompacted() {
        return compacted;
    }


    /**
     * Compare the shards based on the timestamp first, then the created time second
     */
    @Override
    public int compareTo( final Shard o ) {
        if ( o == null ) {
            return 1;
        }

        if ( shardIndex > o.shardIndex ) {
            return 1;
        }

        else if ( shardIndex == o.shardIndex ) {
            if ( createdTime > o.createdTime ) {
                return 1;
            }
            else if ( createdTime < o.createdTime ) {
                return -1;
            }

            else {

                //kind of arbitrary compacted takes precedence
                if ( compacted && !o.compacted ) {
                    return 1;
                }

                else if ( !compacted && o.compacted ){
                    return -1;
                }


            }
            return 0;
        }

        return -1;
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }

        final Shard shard = ( Shard ) o;

        if ( compacted != shard.compacted ) {
            return false;
        }
        if ( createdTime != shard.createdTime ) {
            return false;
        }
        if ( shardIndex != shard.shardIndex ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = ( int ) ( shardIndex ^ ( shardIndex >>> 32 ) );
        result = 31 * result + ( int ) ( createdTime ^ ( createdTime >>> 32 ) );
        result = 31 * result + ( compacted ? 1 : 0 );
        return result;
    }


    @Override
    public String toString() {
        return "Shard{" +
                "shardIndex=" + shardIndex +
                ", createdTime=" + createdTime +
                ", compacted=" + compacted +
                '}';
    }
}
