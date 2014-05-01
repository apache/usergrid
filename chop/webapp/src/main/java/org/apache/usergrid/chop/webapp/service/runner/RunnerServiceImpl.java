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
package org.apache.usergrid.chop.webapp.service.runner;

import org.apache.usergrid.chop.api.Result;
import org.apache.usergrid.chop.api.Runner;
import org.apache.usergrid.chop.api.State;
import org.apache.usergrid.chop.api.StatsSnapshot;
import org.apache.usergrid.chop.client.rest.RestRequests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A basic implementation of RunnerService
 */
public class RunnerServiceImpl implements RunnerService {

    private static final Logger LOG = LoggerFactory.getLogger(RunnerServiceImpl.class);

    public State getState(Runner runner) {
        State state = null;

        try {
            Result result = RestRequests.status(runner);
            state = result.getState();
        } catch (Exception e) {
            LOG.error("Error to get a runner status: ", e);
        }

        return state;
    }

    public StatsSnapshot getStats(Runner runner) {
        StatsSnapshot stats = null;

        try {
            stats = RestRequests.stats(runner);
        } catch (Exception e) {
            LOG.error("Error to get a runner stats: ", e);
        }

        return stats;
    }

}
