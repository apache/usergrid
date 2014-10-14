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


import java.util.List;

import org.apache.usergrid.chop.api.Runner;
import org.apache.usergrid.chop.stack.ICoordinatedCluster;


/**
 * A registry service for Runners.
 */
public interface RunnerRegistry {
    /**
     * Gets the runner instance information from the RunnerRegistry as list of Runner instances.
     *
     * @return the keys mapped to Runner information
     */
    List<Runner> getRunners();

    /**
     * Gets the runner instance information from the RunnerRegistry as a list of Runner instances.
     *
     *
     * @param runner a runner to exclude from results (none if null)
     *
     * @return the keys mapped to Runner instance
     */
    List<Runner> getRunners( Runner runner );

    /**
     * Registers this runner instance by adding its instance information into the
     * RunnerRegistry as a properties file using the following key format:
     *
     * "$RUNNERS_PATH/publicHostname.properties"
     *
     * @param runner the runner's configuration instance to be registered
     */
    void register( Runner runner );

    /**
     * Removes this Runner's registration.
     *
     * @param runner the runners information
     */
    void unregister( Runner runner );


    /**
     * @return all set up clusters list from coordinator
     */
    List<ICoordinatedCluster> getClusters();
}

