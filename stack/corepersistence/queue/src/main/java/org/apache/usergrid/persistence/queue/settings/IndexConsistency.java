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
 * This class describes the consistency rules when returning results set between C* and ES
 *
 * Created by peterajohnson on 10/30/17.
 */
public enum IndexConsistency {

    STRICT("strict"),       // Result canidate must be exact match to be returned in result set
    LATEST("latest");       // Result canidate must be exact match OR most recent version to be returned in result set

    private String name;

    private static final Map<String,IndexConsistency> NAME_MAP;

    static {
        Map<String,IndexConsistency> map = new HashMap<>();
        for (IndexConsistency instance : IndexConsistency.values()) {
            map.put(instance.getName(),instance);
        }
        NAME_MAP = Collections.unmodifiableMap(map);
    }

    IndexConsistency(String name) {
        this.name = name;
    }

    public static IndexConsistency get(String name) {
        IndexConsistency queueIndexingStrategy =  NAME_MAP.get(name);
        if (queueIndexingStrategy == null) {
            return STRICT;
        }
        return queueIndexingStrategy;
    }


    public String getName() {
        return this.name;
    }

}
