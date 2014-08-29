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
package org.apache.usergrid.chop.runner.drivers;


import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;


/**
 * Executes and tracks chop run statistics.
 */
public abstract class Tracker {
    private static final Logger LOG = LoggerFactory.getLogger( Tracker.class );
    public static final int INVALID_TIME = -1;

    protected final Class<?> testClass;
    private final IResultsLog resultsLog;

    private long startTime = System.currentTimeMillis();
    private long stopTime = INVALID_TIME;

    // the total number of test methods run (will be >= actualIterations)
    private final AtomicInteger totalTestsRun = new AtomicInteger( 0 );
    // the total number of failures that resulted
    private final AtomicLong failures = new AtomicLong( 0 );
    // the total number of run iterations on the test class
    private final AtomicLong actualIterations = new AtomicLong( 0 );
    // the total number of ignored test methods
    private final AtomicLong ignores = new AtomicLong( 0 );
    // the total run time of the tests minus setup time
    private final AtomicLong totalRunTime = new AtomicLong( 0 );
    // the max run time encountered for a test class run
    private long maxTime = Long.MIN_VALUE;
    // the min run time encountered for a test class run
    private long minTime = Long.MAX_VALUE;
    // the average run time encountered across all test class runs
    private long meanTime = 0;
    // by default we have started but we just want to detect a stop
    private AtomicBoolean isStarted = new AtomicBoolean( true );


    protected Tracker( Class<?> testClass ) {
        this.testClass = testClass;

        try {
            resultsLog = new ResultsLog( this );
            resultsLog.open();
        }
        catch ( IOException e ) {
            LOG.error( "Failed to open the results log.", e );
            throw new RuntimeException( "Could not open results log.", e );
        }
    }


    @JsonProperty
    public Class<?> getTestClass() {
        return testClass;
    }


    public Result execute() {
        Preconditions.checkState( isStarted.get(), "Cannot execute a tracker that has not started!" );

        Result result = new JUnitCore().run( testClass );
        long runTime = result.getRunTime();

        // collect some statistics
        maxTime = Math.max( maxTime, runTime );
        minTime = Math.min( minTime, runTime );
        long timesRun = actualIterations.incrementAndGet();
        long totalTime = totalRunTime.addAndGet( runTime );
        totalTestsRun.addAndGet( result.getRunCount() );
        meanTime = totalTime / timesRun;

        if ( ! result.wasSuccessful() ) {
            failures.addAndGet( result.getFailureCount() );
            ignores.addAndGet( result.getIgnoreCount() );
        }
        resultsLog.write( result );
        return result;
    }


    @JsonProperty
    public abstract long getDelay();

    @JsonProperty
    public abstract int getThreads();

    @JsonProperty
    public abstract boolean getSaturate();


    @JsonProperty
    public long getDuration() {
        Preconditions.checkState( stopTime != INVALID_TIME,
                "The stopTime has not been set: check that the test completed." );

        return stopTime - startTime;
    }


    @JsonProperty
    public long getStartTime() {
        return startTime;
    }


    @JsonProperty
    public long getStopTime() {
        Preconditions.checkState( stopTime != INVALID_TIME,
                "The stopTime has not been set: check that the test completed." );

        return stopTime;
    }


    void stop() {
        Preconditions.checkState( isStarted.get(), "Cannot stop Tracker which has not started." );
        stopTime = System.currentTimeMillis();

        try {
            resultsLog.close();
        }
        catch ( IOException e ) {
            LOG.error( "Failed to close the results log", e );
        }
    }


    File getResultsFile() {
        return new File( resultsLog.getPath() );
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
    public long getActualTime() {
        return stopTime - startTime;
    }


    @JsonProperty
    public long getActualIterations() {
        return actualIterations.get();
    }


    @JsonProperty
    public long getTotalTestsRun() {
        return totalTestsRun.get();
    }


    @JsonProperty
    public long getFailures() {
        return failures.get();
    }


    @JsonProperty
    public long getIgnores() {
        return ignores.get();
    }


    @JsonProperty
    public long getTotalRunTime() {
        return totalRunTime.get();
    }


    @JsonProperty
    public abstract int getPercentCompleted();
}
