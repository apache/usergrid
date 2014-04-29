/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.core.consistency;


import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


/**
 * Configuration for the consistency queueing system
 */
public interface ConsistencyFig extends GuicyFig {


    public static final String TIMEOUT_TASK_TIME = "usergrid.graph.timeout.task.time";

    public static final String TIMEOUT_SIZE = "usergrid.graph.timeout.page.size";


    public static final String REPAIR_TIMEOUT = "usergrid.graph.repair.timeout";




    @Default("500")
    @Key(TIMEOUT_TASK_TIME)
    long getTaskLoopTime();


    @Default("100")
    @Key(TIMEOUT_SIZE)
    int getTimeoutReadSize();


    @Default("10000")
    @Key(REPAIR_TIMEOUT)
    int getRepairTimeout();


}
