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

import org.apache.usergrid.chop.api.StatsSnapshot;


/**
 * A driver for a chop applied to a test class. Drivers simply execute the chop
 * while applying the chop specification criteria.
 */
public interface IDriver<T extends Tracker> {
    /**
     * Gets a snapshot of the statistics of the in progress chop.
     *
     * @return a snapshot of the chop statistics
     */
    StatsSnapshot getChopStats();

    /**
     * Gets the tracker associated with this driver.
     *
     * @return this driver's chop tracker
     */
    T getTracker();

    /**
     * If the driver is not running, this method can be called to get the
     * detailed results associated with the chop.
     *
     * @return the detailed results file of the completed/stopped chop
     */
    File getResultsFile();

    /**
     * Checks to see if this driver has successfully completed on it's own.
     *
     * @return true if this driver completed without being stopped
     */
    boolean isComplete();

    /**
     * Checks to see if this driver is still running.
     *
     * @return true if this driver is still running
     */
    boolean isRunning();

    /**
     * Checks to see if this driver has been prematurely stopped. Note that
     * being prematurely stopped is not the same as being in the completed
     * state where the driver stopped after completing the chop naturally.
     *
     * @return true if this driver was prematurely stopped
     */
    boolean isStopped();

    /**
     * Start this driver.
     */
    void start();

    /**
     * Prematurely stop this driver.
     */
    void stop();
}
