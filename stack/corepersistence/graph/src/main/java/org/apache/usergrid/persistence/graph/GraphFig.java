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
package org.apache.usergrid.persistence.graph;


import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


/**
 *
 *
 */
public interface GraphFig extends GuicyFig {


    public static final String SCAN_PAGE_SIZE = "usergrid.graph.scan.page.size";

    public static final String REPAIR_CONCURRENT_SIZE = "usergrid.graph.repair.concurrent.size";

    public static final String REPAIR_TIMEOUT = "usergrid.graph.repair.timeout";

    public static final String TIMEOUT_SIZE = "usergrid.graph.timeout.page.size";

    public static final String TIMEOUT_TASK_TIME = "usergrid.graph.timeout.task.time";

    public static final String READ_CL = "usergrid.graph.read.cl";

    public static final String WRITE_CL = "usergrid.graph.write.cl";

    public static final String WRITE_TIMEOUT = "usergrid.graph.write.timeout";

    public static final String READ_TIMEOUT = "usergrid.graph.read.timeout";

    @Default( "1000" )
    @Key(SCAN_PAGE_SIZE)
    int getScanPageSize();

    @Default("CL_QUORUM")
    @Key(READ_CL)
    String getReadCL();

    @Default("CL_QUORUM")
    @Key(WRITE_CL)
    String getWriteCL();

    @Default( "10000" )
    @Key( WRITE_TIMEOUT )
    int getWriteTimeout();

    /**
     * Get the read timeout (in milliseconds) that we should allow when reading from the data source
     */
    @Default("10000")
    @Key(READ_TIMEOUT)
    int getReadTimeout();

    @Default("100")
    @Key(TIMEOUT_SIZE)
    int getTimeoutReadSize();

    @Default("500")
    @Key(TIMEOUT_TASK_TIME)
    long getTaskLoopTime();

    @Default("5")
    @Key(REPAIR_CONCURRENT_SIZE)
    int getRepairConcurrentSize();

    @Default("10000")
    @Key(REPAIR_TIMEOUT)
    int getRepairTimeout();
}

