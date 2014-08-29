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


/** Constants ... */
public interface Constants {
    /** the project HTTP parameter */
    String PARAM_PROJECT = "project";

    /** Stack definition json file name */
    String STACK_JSON = "stack.json";

    /** the default name to use for the runner's jar file */
    String RUNNER_JAR = "runner.jar";

    /** the number of characters of the UUID to use for UUID path component */
    int CHARS_OF_UUID = 8;

    /** the name of the project json file */
    String PROJECT_FILE = "project.properties";

    /** the path to the runners */
    String RUNNERS_PATH = "runners";

    /** path to the tests */
    String TESTS_PATH = "tests";

    /** the suffix used for the run summary json file */
    String SUMMARY_SUFFIX = "-summary.json";

    /** the suffix used for the run results json file */
    String RESULTS_SUFFIX = "-results.json";

    /** IterationChop iterations default */
    long DEFAULT_ITERATIONS = 1000L;

    /** TimeChop iterations default */
    long DEFAULT_TIME = 30000L;

    /** Chop threads default */
    int DEFAULT_THREADS = 10;

    /** Chop saturate default */
    boolean DEFAULT_SATURATE = false;

    /** Chop delay default */
    long DEFAULT_DELAY = 0;
    String PRETTY_PRINT_RESULTS = "pretty.print.results";
}
