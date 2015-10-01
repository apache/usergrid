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
package org.apache.usergrid.chop.spi;


import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;

import org.apache.usergrid.chop.api.Project;
import org.apache.usergrid.chop.api.Runner;
import org.apache.usergrid.chop.api.Summary;

/**
 * Manages run information.
 */
public interface RunManager {
    /**
     * Stores the summary and results file for a chop test run into the store.
     *
     * @param project the project associated with the run
     * @param summary the summary information associated with the test run
     * @param resultsFile the results log file
     * @param testClass the chopped test class
     */
    void store( Project project, Summary summary, File resultsFile, Class<?> testClass )
            throws FileNotFoundException, MalformedURLException;

    /**
     * Checks to see if a runner has deposited run summary information for a chopped test in
     * the store.
     *
     * @param runner the runner to check for chop completion
     * @param project the project being run
     * @param runNumber the run number
     * @param testClass the chopped test to check for completion on
     * @return true if the summary information has been deposited, false otherwise
     */
    boolean hasCompleted( Runner runner, Project project, int runNumber, Class<?> testClass );

    /**
     * Checks the store to find the next available run number starting at 1. This method
     * needs to be used with extreme caution. It should only be used when starting up a
     * runner's controller. The intention is to be able to enable Judo Chop to restart
     * runner containers between runs to refresh the Tomcat container.
     *
     * WARNING: It should not be used at any time other than runner initialization since if
     * used with many runners concurrently during test dumps to the store, it could result
     * in race conditions. During runner initialization this is not a possibility on the
     * same project.
     *
     * @param project   the project configuration
     * @return          the next available run number
     */
    int getNextRunNumber( Project project );

}
