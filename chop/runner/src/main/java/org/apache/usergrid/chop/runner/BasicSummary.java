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
package org.apache.usergrid.chop.runner;


import java.util.UUID;

import org.apache.usergrid.chop.api.Summary;
import org.apache.usergrid.chop.runner.drivers.TimeTracker;
import org.apache.usergrid.chop.runner.drivers.Tracker;
import org.apache.usergrid.chop.runner.drivers.IterationTracker;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;


/**
 * Summary information about a single chop run associated with a specific class and
 * a specific chop type. This is really a value object used for transmitting and
 * storing information about a run after a run completes rather than to query the
 * run while it is RUNNING.
 *
 * Feel free to stuff any kind of cumulative summary information into this entity.
 * It might be nice to include some percentile information as well.
 */
public class BasicSummary implements Summary {
    private final int runNumber;

    private long iterations;
    private long totalTestsRun;
    private String testName;
    private String chopType;
    private int threads;
    private long delay;
    private long time;
    private long actualTime;
    private long minTime;
    private long maxTime;
    private long meanTime;
    private long failures;
    private long ignores;
    private long startTime;
    private long stopTime;
    private boolean saturate = false;

    private final UUID runId = UUID.randomUUID();

    public BasicSummary( int runNumber ) {
        this.runNumber = runNumber;
    }

    @Override
    public String getRunId() {
        return runId.toString();
    }

    public void setIterationTracker( IterationTracker iterationTracker ) {
        iterations = iterationTracker.getIterationChop().iterations();
        chopType = "IterationChop";
        setTracker( iterationTracker );
    }


    public void setTimeTracker( TimeTracker timeTracker ) {
        time = timeTracker.getTimeChop().time();
        chopType = "TimeChop";
        setTracker( timeTracker );
    }


    private void setTracker( Tracker tracker ) {

        Preconditions.checkState( tracker.getStopTime() != Tracker.INVALID_TIME,
                "The stop time cannot be invalid." );
        Preconditions.checkState( tracker.getStartTime() != Tracker.INVALID_TIME,
                "The start time cannot be invalid." );

        saturate = tracker.getSaturate();
        totalTestsRun = tracker.getTotalTestsRun();
        testName = tracker.getTestClass().getName();
        threads = tracker.getThreads();
        delay = tracker.getDelay();
        actualTime = tracker.getActualTime();
        minTime = tracker.getMinTime();
        maxTime = tracker.getMaxTime();
        meanTime = tracker.getMeanTime();
        failures = tracker.getFailures();
        ignores = tracker.getIgnores();
        startTime = tracker.getStartTime();
        stopTime = tracker.getStopTime();
    }


    @JsonProperty
    public int getRunNumber() {
        return runNumber;
    }


    @Override
    @JsonProperty
    public long getIterations() {
        return iterations;
    }


    @Override
    @JsonProperty
    public long getTotalTestsRun() {
        return totalTestsRun;
    }


    @JsonProperty
    public String getTestName() {
        return testName;
    }


    @Override
    @JsonProperty
    public String getChopType() {
        return chopType;
    }


    @Override
    @JsonProperty
    public int getThreads() {
        return threads;
    }


    @Override
    @JsonProperty
    public long getDelay() {
        return delay;
    }


    @Override
    @JsonProperty
    public long getTime() {
        return time;
    }


    @Override
    @JsonProperty
    public long getActualTime() {
        return actualTime;
    }


    @Override
    @JsonProperty
    public long getMinTime() {
        return minTime;
    }


    @Override
    @JsonProperty
    public long getMaxTime() {
        return maxTime;
    }


    @Override
    @JsonProperty
    public long getAvgTime() {
        return meanTime;
    }


    @Override
    @JsonProperty
    public long getFailures() {
        return failures;
    }


    @Override
    @JsonProperty
    public long getIgnores() {
        return ignores;
    }


    @Override
    @JsonProperty
    public long getStartTime() {
        return startTime;
    }


    @Override
    @JsonProperty
    public long getStopTime() {
        return stopTime;
    }


    @Override
    public boolean getSaturate() {
        return saturate;
    }
}
