/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.core.rx;


import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


/**
 *
 */
@FigSingleton
public interface RxSchedulerFig extends GuicyFig {


    /**
     * Amount of time in milliseconds to wait when ES rejects our request before retrying.  Provides simple
     * backpressure
     */
    String IO_SCHEDULER_THREADS = "scheduler.io.threads";


    /**
     * Amount of time in milliseconds to wait when ES rejects our request before retrying.  Provides simple
     * backpressure
     */
    String IO_SCHEDULER_NAME = "scheduler.io.poolName";

    /**
     * The number of threads to use when importing entities into result sets
     */
    String IO_IMPORT_THREADS = "scheduler.import.threads";




    @Default( "100" )
    @Key( IO_SCHEDULER_THREADS )
    int getMaxIoThreads();

    @Default( "Usergrid-RxIOPool" )
    @Key(IO_SCHEDULER_NAME)
    String getIoSchedulerName();

    @Default("20")
    @Key( IO_IMPORT_THREADS)
    int getImportThreads();



}
