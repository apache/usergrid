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


import org.apache.usergrid.chop.api.Signal;
import org.apache.usergrid.chop.api.StatsSnapshot;
import org.apache.usergrid.chop.api.Project;
import org.apache.usergrid.chop.api.State;


/**
 * The controller is responsible for finding chop annotated test classes under
 * a base package and running the suite of chops defined.
 */
public interface IController {
    /**
     * Resets this IController if it has been prematurely stopped.
     */
    void reset();

    /**
     * If this IController was stopped prematurely and is in the State.STOPPED
     * state then this call returns true.
     *
     * @return true if in the State.STOPPED state
     */
    boolean needsReset();

    /**
     * Gets a snapshot of the statistics associated with the currently running chop.
     *
     * @return a snapshot of the statistics
     */
    StatsSnapshot getCurrentChopStats();

    /**
     * Gets the State of this IController.
     *
     * @return the current state
     */
    State getState();

    /**
     * Checks whether or not this IController is in the State.RUNNING state.
     *
     * @return true if in the State.RUNNING state
     */
    boolean isRunning();

    /**
     * Starts this IController which begins running the suite of chops.
     */
    void start();

    /**
     * Prematurely stops this IController. The IController will naturally stop
     * itself after running all chops to fall back into the State.READY state.
     */
    void stop();

    /**
     * Takes a signal parameter to determine whether to start, stop, reset etc.
     */
    void send( Signal signal );

    /**
     * The project that is currently being run.
     *
     * @return the project being chopped up
     */
    Project getProject();
}
