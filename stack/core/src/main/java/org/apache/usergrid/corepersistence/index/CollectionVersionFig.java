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

package org.apache.usergrid.corepersistence.index;

import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


/**
 * Collection version cache config
 */
@FigSingleton
public interface CollectionVersionFig extends GuicyFig {

    String CACHE_SIZE = "usergrid.collection_version_cache_size";
    String CACHE_TIMEOUT_MS = "usergrid.collection_version_cache_timeout_ms";
    String TIME_BETWEEN_DELETES_MS = "usergrid.collection_version_time_between_deletes_ms";
    String DELETES_PER_EVENT = "usergrid.collection_deletes_per_event";

    @Key(CACHE_SIZE)
    @Default("500")
    int getCacheSize();

    @Key(CACHE_TIMEOUT_MS)
    @Default("2000")
    int getCacheTimeout();

    @Key(TIME_BETWEEN_DELETES_MS)
    @Default("60000")
    long getTimeBetweenDeletes();

    @Key(DELETES_PER_EVENT)
    @Default("10000")
    int getDeletesPerEvent();

}
