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

package org.apache.usergrid.persistence.collection.scheduler;


import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


/**
 *
 */
@FigSingleton
public interface CollectionSchedulerFig extends GuicyFig {


    /**
     * Amount of threads to use in async processing
     */
    String COLLECTION_SCHEDULER_THREADS = "scheduler.collection.threads";


    /**
     * Name of pool to use when performing scheduling
     */
    String COLLECTION_SCHEDULER_NAME = "scheduler.collection.poolName";


    @Default( "20" )
    @Key( COLLECTION_SCHEDULER_THREADS )
    int getMaxIoThreads();

    @Default( "Usergrid-Collection-Pool" )
    @Key( COLLECTION_SCHEDULER_NAME )
    String getIoSchedulerName();
}
