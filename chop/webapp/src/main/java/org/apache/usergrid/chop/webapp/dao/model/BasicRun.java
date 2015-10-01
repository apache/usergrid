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
package org.apache.usergrid.chop.webapp.dao.model;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.usergrid.chop.api.Run;
import org.apache.usergrid.chop.webapp.elasticsearch.Util;

import java.util.Map;


/**
 * Corresponds to the summary of a single chop test run of a test class for a commit
 */
public class BasicRun implements Run {

    private String id;
    private String commitId;
    private String runner;
    private int runNumber;
    private String testName;

    private String chopType;
    private long iterations;
    private long totalTestsRun;
    private int threads;
    private long delay;
    private long time;
    private long actualTime;
    private long minTime;
    private long maxTime;
    private long avgTime;
    private long failures;
    private long ignores;
    private long startTime;
    private long stopTime;
    private boolean saturate;


    /**
     * @param id
     * @param commitId
     * @param runner    hostname of the runner
     * @param runNumber
     * @param testName
     */
    public BasicRun( String id, String commitId, String runner, int runNumber, String testName ) {
        this.commitId = commitId;
        this.runner = runner;
        this.runNumber = runNumber;
        this.testName = testName;
        this.id = id;
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append( commitId )
                .append( runner )
                .append( runNumber )
                .append( testName )
                .toHashCode();
    }


    public void copyJson( Map<String, Object> json ) {
        setChopType( Util.getString( json, "chopType" ) );
        setIterations( Util.getInt( json, "actualIterations" ) );
        setTotalTestsRun( Util.getInt( json, "totalTestsRun" ) );
        setThreads( Util.getInt( json, "threads" ) );
        setDelay( Util.getInt( json, "delay" ) );
        setTime( Util.getInt( json, "time" ) );
        setActualTime( Util.getInt( json, "actualTime" ) );
        setMinTime( Util.getInt( json, "minTime" ) );
        setMaxTime( Util.getInt( json, "maxTime" ) );
        setAvgTime( Util.getInt( json, "meanTime" ) );
        setFailures( Util.getInt( json, "failures" ) );
        setIgnores( Util.getInt( json, "ignores" ) );
        setStartTime( Util.getLong( json, "startTime" ) );
        setStopTime( Util.getLong( json, "stopTime" ) );
        setSaturate( Util.getBoolean( json, "saturate" ) );
    }


    public String getId() {
        return id;
    }


    public String getCommitId() {
        return commitId;
    }


    public String getRunner() {
        return runner;
    }


    public int getRunNumber() {
        return runNumber;
    }


    @Override
    public String getTestName() {
        return testName;
    }


    public long getIterations() {
        return iterations;
    }


    public long getTotalTestsRun() {
        return totalTestsRun;
    }


    public String getChopType() {
        return chopType;
    }


    public int getThreads() {
        return threads;
    }


    public long getDelay() {
        return delay;
    }


    public long getTime() {
        return time;
    }


    public long getActualTime() {
        return actualTime;
    }


    public long getMinTime() {
        return minTime;
    }


    public long getMaxTime() {
        return maxTime;
    }


    public long getAvgTime() {
        return avgTime;
    }


    public long getFailures() {
        return failures;
    }


    public long getIgnores() {
        return ignores;
    }


    public long getStartTime() {
        return startTime;
    }


    public long getStopTime() {
        return stopTime;
    }


    public boolean getSaturate() {
        return saturate;
    }


    public void setIterations( long iterations ) {
        this.iterations = iterations;
    }


    public void setTotalTestsRun( long totalTestsRun ) {
        this.totalTestsRun = totalTestsRun;
    }


    public void setChopType( String chopType ) {
        this.chopType = chopType;
    }


    public void setThreads( int threads ) {
        this.threads = threads;
    }


    public void setDelay( long delay ) {
        this.delay = delay;
    }


    public void setTime( long time ) {
        this.time = time;
    }


    public void setActualTime( long actualTime ) {
        this.actualTime = actualTime;
    }


    public void setMinTime( long minTime ) {
        this.minTime = minTime;
    }


    public void setMaxTime( long maxTime ) {
        this.maxTime = maxTime;
    }


    public void setAvgTime( long avgTime ) {
        this.avgTime = avgTime;
    }


    public void setFailures( long failures ) {
        this.failures = failures;
    }


    public void setIgnores( long ignores ) {
        this.ignores = ignores;
    }


    public void setStartTime( long startTime ) {
        this.startTime = startTime;
    }


    public void setStopTime( long stopTime ) {
        this.stopTime = stopTime;
    }


    public void setSaturate( boolean saturate ) {
        this.saturate = saturate;
    }


    public String toString() {
        return new ToStringBuilder( this, ToStringStyle.SHORT_PREFIX_STYLE )
                .append( "id", id )
                .append( "runNumber", runNumber )
                .append( "commitId", commitId )
                .append( "actualTime", actualTime )
                .append( "minTime", minTime )
                .append( "maxTime", maxTime )
                .append( "avgTime", avgTime )
                .append( "failures", failures )
                .append( "ignores", ignores )
                .append( "runner", runner )
                .append( "testName", testName )
                .append( "chopType", chopType )
                .append( "iterations", iterations )
                .append( "totalTestsRun", totalTestsRun )
                .append( "threads", threads )
                .append( "delay", delay )
                .append( "time", time )
                .append( "startTime", startTime )
                .append( "stopTime", stopTime )
                .append( "saturate", saturate )
                .toString();
    }
}
