/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.api;


import com.fasterxml.jackson.annotation.JsonProperty;


/** ... */
public class StatsSnapshot {
    private long testClassRuns;
    private long maxTime;
    private long minTime;
    private long meanTime;
    private boolean running;
    private long startTime;
    private int percentageComplete;


    @SuppressWarnings( "UnusedDeclaration" )
    public StatsSnapshot() {
    }


    public StatsSnapshot( long testClassRuns, long maxTime, long minTime, long meanTime,
                          boolean running, long startTime, int percentageComplete ) {
        this.testClassRuns = testClassRuns;
        this.maxTime = maxTime;
        this.minTime = minTime;
        this.meanTime = meanTime;
        this.running = running;
        this.startTime = startTime;
        this.percentageComplete = percentageComplete;
    }


    @JsonProperty
    public int getPercentageComplete() {
        return percentageComplete;
    }


    @SuppressWarnings( "UnusedDeclaration" )
    public void setPercentageComplete( int percentageComplete ) {
        this.percentageComplete = percentageComplete;
    }


    @JsonProperty
    public long getTestClassRuns() {
        return testClassRuns;
    }


    @SuppressWarnings( "UnusedDeclaration" )
    public void setTestClassRuns( long testClassRuns ) {
        this.testClassRuns = testClassRuns;
    }


    @JsonProperty
    public long getMaxTime() {
        return maxTime;
    }


    @SuppressWarnings( "UnusedDeclaration" )
    public void setMaxTime( long maxTime ) {
        this.maxTime = maxTime;
    }


    @JsonProperty
    public long getMinTime() {
        return minTime;
    }


    @SuppressWarnings( "UnusedDeclaration" )
    public void setMinTime( long minTime ) {
        this.minTime = minTime;
    }


    @JsonProperty
    public long getMeanTime() {
        return meanTime;
    }


    @SuppressWarnings( "UnusedDeclaration" )
    public void setMeanTime( long meanTime ) {
        this.meanTime = meanTime;
    }


    @JsonProperty
    public boolean isRunning() {
        return running;
    }


    public void setRunning( boolean running ) {
        this.running = running;
    }


    @JsonProperty
    public long getStartTime() {
        return startTime;
    }


    @SuppressWarnings( "UnusedDeclaration" )
    public void setStartTime( long startTime ) {
        this.startTime = startTime;
    }
}
