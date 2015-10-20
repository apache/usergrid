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
package org.apache.usergrid.persistence.core.astyanax;


import com.netflix.astyanax.model.ConsistencyLevel;


/**
 *
 * Wraps our fig configuration since it doesn't support enums yet.  Once enums are supported, remove this wrapper and
 * replace with the fig configuration itself
 *
 */
public interface CassandraConfig {

    /**
     * Get the currently configured ReadCL
     * @return
     */
    ConsistencyLevel getReadCL();

    /**
     * Get the currently configured ReadCL that is more consitent than getReadCL
     * @return
     */
    ConsistencyLevel getConsistentReadCL();

    /**
     * Get the currently configured write CL
     * @return
     */
    ConsistencyLevel getWriteCL();

    /**
     * Return the number of shards that has been set in the property file
     * @return
     */
    int[] getShardSettings();


}
