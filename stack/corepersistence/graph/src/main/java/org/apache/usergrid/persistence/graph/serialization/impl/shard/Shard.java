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


import com.google.common.base.Optional;
import org.apache.usergrid.persistence.graph.Edge;

public class Shard implements Comparable<Shard> {


    /**
     * The minimum shard a shardIndex can possibly be set to
     */
    public static final Shard MIN_SHARD = new Shard(0, 0, true);

    private final long shardIndex;
    private final long createdTime;
    private final boolean compacted;
    private Optional<Edge> shardEnd;


    public Shard( final long shardIndex, final long createdTime, final boolean compacted ) {
        this.shardIndex = shardIndex;
        this.createdTime = createdTime;
        this.compacted = compacted;
        this.shardEnd = Optional.absent();
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
     * Returns true if this is the minimum shard
     * @return
     */
    public boolean isMinShard(){
        return shardIndex == MIN_SHARD.shardIndex;
    }

    public void setShardEnd(final Optional<Edge> shardEnd) {
        this.shardEnd = shardEnd;
    }

    public Optional<Edge> getShardEnd() {
        return shardEnd;
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

        StringBuilder string = new StringBuilder();
        string.append("Shard{ ");
        string.append("shardIndex=").append(shardIndex);
        string.append(", createdTime=").append(createdTime);
        string.append(", compacted=").append(compacted);
        string.append(", shardEndTimestamp=");
        if(shardEnd.isPresent()){
            string.append(shardEnd.get().getTimestamp());
        }else{
            string.append("null");
        }
        string.append(" }");

        return string.toString();
    }
}
