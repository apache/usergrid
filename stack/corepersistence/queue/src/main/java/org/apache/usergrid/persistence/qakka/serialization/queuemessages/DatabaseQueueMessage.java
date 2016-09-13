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

package org.apache.usergrid.persistence.qakka.serialization.queuemessages;

import java.util.UUID;


public class DatabaseQueueMessage {

    public enum Type {
        DEFAULT, INFLIGHT
    }

    private final String queueName;
    private final String region;
    private final Long queuedAt;
    private final UUID messageId;
    private final UUID queueMessageId;

    private Type type;
    private Long inflightAt;

    private Long shardId;


    public DatabaseQueueMessage(
            final UUID messageId,
            final Type type,
            final String queueName,
            final String region,
            final Long shardId,
            final Long queuedAt,
            final Long inflightAt,
            UUID queueMessageId){

        this.messageId = messageId;
        this.type = type;
        this.queueName = queueName;
        this.region = region;
        this.shardId = shardId;
        this.queuedAt = queuedAt;
        this.inflightAt = inflightAt;
        this.queueMessageId = queueMessageId;

    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getQueueName() {
        return queueName;
    }

    public String getRegion() {
        return region;
    }

    public Long getShardId() {
        return shardId;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public Type getType() {
        return type;
    }

    public Long getQueuedAt() {
        return queuedAt;
    }

    public UUID getQueueMessageId() {
        return queueMessageId;
    }

    public Long getInflightAt() {
        return inflightAt;
    }

    public void setInflightAt(Long inflightAt) {
        this.inflightAt = inflightAt;
    }

    public void setShardId(Long shardId) {
        this.shardId = shardId;
    }



    @Override
    public int hashCode() {
        int result = queueName.hashCode();
        result = ( 31 * result ) + region.hashCode();
        result = ( 31 * result ) + (int)( shardId != null ? shardId : 0L );
        result = ( 31 * result ) + messageId.hashCode();
        result = ( 31 * result ) + type.hashCode();

        return result;
    }

    @Override
    public boolean equals(Object obj) {

        if( this == obj){
            return true;
        }

        if( !(obj instanceof DatabaseQueueMessage)){
            return false;
        }

        DatabaseQueueMessage that = (DatabaseQueueMessage) obj;

        if( !this.queueName.equalsIgnoreCase(that.queueName)){
            return false;
        }
        if( !this.region.equalsIgnoreCase(that.region)){
            return false;
        }
        if( this.shardId != that.shardId){
            return false;
        }
        if( !messageId.equals(that.messageId)){
            return false;
        }
        if( !type.equals(that.type)){
            return false;
        }

        return true;

    }


}
