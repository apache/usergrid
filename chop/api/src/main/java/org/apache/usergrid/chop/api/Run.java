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

/**
 * A run of a specific version of a Maven Module test class under test.
 */
public interface Run {

    String getId();

    /**
     * Returns a specific commit version of a Maven Module under test.
     */
    @JsonProperty
    String getCommitId();

    /**
     * Returns a runner.
     */
    @JsonProperty
    String getRunner();

    /**
     * Gets the fully qualified name of the Class that was chopped.
     *
     * @return the name of the chopped test
     */
    @JsonProperty
    String getTestName();

    /**
     * Gets the run number. Each start that is issued against the same
     * Runner will increment the run number. The results of each run
     * are kept separate with a path component being the run number.
     *
     * @return the run number
     */
    @JsonProperty
    int getRunNumber();

    /**
     * Gets the number of iterations used in an IterationChop if the
     * chop summary is for an IterationChop, otherwise this value for
     * a TimeChop Summary will always be zero.
     *
     * @return the number of iterations for IterationChops or zero for
     * TimeChop summaries
     */
    @JsonProperty
    long getIterations();

    /**
     * Gets the total number of test cases that were executed. This is
     * a sum of all the test cases executed by all the threads in the
     * chop.
     *
     * @return the sum of all the tests cases executed by all threads
     */
    @JsonProperty
    long getTotalTestsRun();



    /**
     * Gets the chop type used to generate this summary information, either
     * being the String "TimeChop" or the String "IterationChop".
     *
     * @return the type of the chop associated with this summary
     */
    @JsonProperty
    String getChopType();

    /**
     * Gets the number of threads used to conduct the chop that was specified
     * by the Judo Chop annotation that was used.
     *
     * @return the number of threads used to chop it up
     */
    @JsonProperty
    int getThreads();

    /**
     * Gets the delay in milliseconds between test class iterations specified
     * either in the {@link TimeChop#delay()} or the {@link IterationChop#delay()}.
     *
     * @return the delay in milliseconds between test class iterations
     */
    @JsonProperty
    long getDelay();

    /**
     * Gets the time specification in a {@link TimeChop} if that was used to generate the
     * summary or zero if an {@link IterationChop} was used.
     *
     * @return the time specification of the TimeChop or zero if IterationChop was used
     */
    @JsonProperty
    long getTime();

    /**
     * Gets the wall clock time it took for the chop to run.
     *
     * @return the wall clock time for the chop to run
     */
    @JsonProperty
    long getActualTime();

    /**
     * Gets the minimum time it took for a test iteration to run in a TimeChop or an
     * IterationChop.
     *
     * @return the minimum time it took for a test iteration to run
     */
    @JsonProperty
    long getMinTime();

    /**
     * Gets the maximum time it took for a test iteration to run in a TimeChop or an
     * IterationChop.
     *
     * @return the maximum time it took for a test iteration to run
     */
    @JsonProperty
    long getMaxTime();

    /**
     * Gets the mean time it took for a test iteration to run in a TimeChop or an
     * IterationChop.
     *
     * @return the avg time it took for a test iteration to run
     */
    @JsonProperty
    long getAvgTime();

    /**
     * Gets the number of JUnit test failures that occurred during the chop.
     *
     * @return the number of JUnit test failures
     */
    @JsonProperty
    long getFailures();

    /**
     * Gets the number of JUnit tests that were ignored while chopin it up.
     *
     * @return the number of JUnit tests that were ignored
     */
    @JsonProperty
    long getIgnores();

    /**
     * Gets the time in milliseconds since 1/1/1970 UTC when the chop was
     * started.
     *
     * @return the start time in milliseconds
     */
    @JsonProperty
    long getStartTime();

    /**
     * Gets the time in milliseconds since 1/1/1970 UTC when the chop completed.
     *
     * @return the completion time in milliseconds
     */
    @JsonProperty
    long getStopTime();

    /**
     * Gets whether or not a saturation test was performed first to determine the
     * number of threads to use. This is the same value as the Chop saturation
     * parameter.
     *
     * @return true if a saturation thread count was first determined before running
     * the chop, false otherwise
     */
    @JsonProperty
    boolean getSaturate();

}
