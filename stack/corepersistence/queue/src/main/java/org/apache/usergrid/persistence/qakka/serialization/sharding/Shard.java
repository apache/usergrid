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

package org.apache.usergrid.persistence.qakka.serialization.sharding;


import java.util.UUID;

public class Shard {

    public enum Type {

        DEFAULT, INFLIGHT
    }

    private String queueName;
    private String region;
    private long shardId;
    private Type type;
    private UUID pointer;

    public Shard(final String queueName, final String region, final Type type, final long shardId, UUID pointer){

        this.queueName = queueName;
        this.region = region;
        this.type = type;
        this.shardId = shardId;
        this.pointer = pointer;

    }

    public String getQueueName() {
        return queueName;
    }

    public String getRegion() {
        return region;
    }

    public long getShardId() {
        return shardId;
    }

    public Type getType() {
        return type;
    }

    public UUID getPointer() {
        return pointer;
    }

    public void setPointer(UUID pointer) {
        this.pointer = pointer;
    }

    @Override
    public int hashCode() {
        int result = queueName.hashCode();
        result = ( 31 * result ) + region.hashCode();
        result = ( 31 * result ) + (int)shardId;
        result = ( 31 * result ) + type.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {

        if( this == obj){
            return true;
        }

        if( !(obj instanceof Shard)){
            return false;
        }

        Shard that = (Shard) obj;

        if( !this.queueName.equalsIgnoreCase(that.queueName)){
            return false;
        }
        if( !this.region.equalsIgnoreCase(that.region)){
            return false;
        }
        if( this.shardId != that.shardId){
            return false;
        }
        if( !this.type.equals(that.type)){
            return false;
        }

        return true;

    }

}
