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

package org.apache.usergrid.persistence.qakka.serialization.queues;

public class DatabaseQueue {

    private final String name;
    private final String regions;

    private String defaultDestinations;
    private Long defaultDelayMs;
    private Integer retryCount;
    private Integer handlingTimeoutSec;
    private String deadLetterQueue;


    public DatabaseQueue(final String name,
                         final String regions,
                         final String defaultDestinations,
                         final Long defaultDelayMs,
                         final Integer retryCount,
                         final Integer handlingTimeoutSec,
                         final String deadLetterQueue ){

        this.name = name;
        this.regions = regions;
        this.defaultDestinations = defaultDestinations;
        this.defaultDelayMs = defaultDelayMs;
        this.retryCount = retryCount;
        this.handlingTimeoutSec = handlingTimeoutSec;
        this.deadLetterQueue = deadLetterQueue;

    }

    public String getName() {
        return name;
    }

    public String getDeadLetterQueue() {
        return deadLetterQueue;
    }

    public Integer getHandlingTimeoutSec() {
        return handlingTimeoutSec;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public Long getDefaultDelayMs() {
        return defaultDelayMs;
    }

    public String getDefaultDestinations() {
        return defaultDestinations;
    }

    public String getRegions() {
        return regions;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = ( 31 * result ) + regions.hashCode();

        return result;
    }

    @Override
    public boolean equals(Object obj) {

        if( this == obj){
            return true;
        }

        if( !(obj instanceof DatabaseQueue)){
            return false;
        }

        DatabaseQueue that = (DatabaseQueue) obj;

        if( !this.name.equalsIgnoreCase(that.name)){
            return false;
        }
        if( !this.regions.equals(that.regions)){
            return false;
        }

        return true;

    }



}
