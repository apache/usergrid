/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.usergrid.perftest.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ...
 */
public class CallStatsSnapshot {
    private final int callCount;
    private final long maxTime;
    private final long minTime;
    private final long meanTime;
    private final boolean running;
    private final long startTime;
    private final long stopTime;


    public CallStatsSnapshot( int callCount, long maxTime, long minTime, long meanTime,
                              boolean running, long startTime, long stopTime ) {
        this.callCount = callCount;
        this.maxTime = maxTime;
        this.minTime = minTime;
        this.meanTime = meanTime;
        this.running = running;
        this.startTime = startTime;
        this.stopTime = stopTime;
    }


    @JsonProperty
    public int getCallCount() {
        return callCount;
    }


    @JsonProperty
    public long getMaxTime() {
        return maxTime;
    }


    @JsonProperty
    public long getMinTime() {
        return minTime;
    }


    @JsonProperty
    public long getMeanTime() {
        return meanTime;
    }


    @JsonProperty
    public boolean isRunning() {
        return running;
    }


    @JsonProperty
    public long getStartTime()
    {
        return startTime;
    }


    @JsonProperty
    public long getStopTime()
    {
        return stopTime;
    }
}
