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
package org.apache.usergrid.persistence.queue.settings;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class describes the paths an index request can take
 * between tomcat and ES.
 *
 * Created by peterajohnson on 10/30/17.
 */
public enum QueueIndexingStrategy {

    NOINDEX("debug_noindex"),        // Do not Index the entity (DEBUG only use for testing)
    DIRECTONLY("debug_directonly"),  // Index request is sent directly to ES and not to AWS

    DIRECT("direct"),          // Index request is sent directly to ES before sync ASW
    SYNC("sync"),              // Index request is sent via a sync AWS to ES
    ASYNC("async"),            // Index request is sent via an async AWS to ES
    CONFIG("config");          // Follow the default setting of the fig

    private String name;

    private static final Map<String,QueueIndexingStrategy> NAME_MAP;

    static {
        Map<String,QueueIndexingStrategy> map = new HashMap<String,QueueIndexingStrategy>();
        for (QueueIndexingStrategy instance : QueueIndexingStrategy.values()) {
            map.put(instance.getName(),instance);
        }
        NAME_MAP = Collections.unmodifiableMap(map);
    }

    QueueIndexingStrategy(String name) {
        this.name = name;
    }

    public static QueueIndexingStrategy get(String name) {
        QueueIndexingStrategy queueIndexingStrategy =  NAME_MAP.get(name);
        if (queueIndexingStrategy == null) {
            return CONFIG;
        }
        return queueIndexingStrategy;
    }


    public String getName() {
        return this.name;
    }

    public boolean shouldSendDirectToES() {
        return  (this == QueueIndexingStrategy.DIRECT || this == QueueIndexingStrategy.DIRECTONLY);
    }

    public boolean shouldSendToAWS() {
        // and is in same region.
        return  (this != QueueIndexingStrategy.DIRECTONLY && this != QueueIndexingStrategy.NOINDEX);
    }

}

