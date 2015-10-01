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


import java.io.IOException;

import org.junit.runner.Result;


/** Logs results as they are produced asynchronously. */
public interface IResultsLog {
    String RESULTS_FILE_KEY = "resultsLog.file";
    String WAIT_TIME_KEY = "resultsLog.waitTime";

    /**
     * Opens the result log.
     *
     * @throws IOException on failures to open the results log file.
     */
    void open() throws IOException;


    /** Closes the result log which also causes a flush. */
    void close() throws IOException;


    /**
     * Truncates the results effectively deleting previous captures.
     *
     * @throws IOException if there are issues truncating the log file.
     */
    void truncate() throws IOException;


    /**
     * Writes a JUnit run result record into the log.
     *
     * @param result the result to write to the log.
     */
    void write( Result result );


    /**
     * Gets the number of results recorded by the log.
     *
     * @return the number of results.
     */
    long getResultCount();


    /**
     * Gets the path to the results log file.
     *
     * @return the local system path to the results log file
     */
    String getPath();
}
