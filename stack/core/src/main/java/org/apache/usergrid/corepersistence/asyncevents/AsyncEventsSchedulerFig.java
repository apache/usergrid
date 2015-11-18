/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.corepersistence.asyncevents;


import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


/**
 *
 */
@FigSingleton
public interface AsyncEventsSchedulerFig extends GuicyFig {


    /**
     * Amount of threads to use in async processing
     */
    String IO_SCHEDULER_THREADS = "scheduler.io.threads";


    /**
     * Name of pool to use when performing scheduling
     */
    String IO_SCHEDULER_NAME = "scheduler.io.poolName";


    /**
     * Amount of threads to use in async processing
     */
    String REPAIR_SCHEDULER_THREADS = "repair.io.threads";


    /**
     * Name of pool to use when performing scheduling
     */
    String REPAIR_SCHEDULER_NAME = "repair.io.poolName";



    @Default( "100" )
    @Key( IO_SCHEDULER_THREADS )
    int getMaxIoThreads();

    @Default( "Usergrid-SQS-Pool" )
    @Key( IO_SCHEDULER_NAME )
    String getIoSchedulerName();


    @Default( "20" )
    @Key( REPAIR_SCHEDULER_THREADS )
    int getMaxRepairThreads();

    @Default( "Usergrid-Repair-Pool" )
    @Key( REPAIR_SCHEDULER_NAME )
    String getRepairPoolName();

}
