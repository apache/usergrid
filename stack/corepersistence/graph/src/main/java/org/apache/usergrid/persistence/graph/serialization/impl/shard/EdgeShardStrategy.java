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

package org.apache.usergrid.persistence.graph.serialization.impl.shard;


import java.util.Iterator;
import java.util.UUID;

import org.apache.usergrid.persistence.model.entity.Id;


public interface EdgeShardStrategy {

    /**
     * Get the shard key used for writing this shard.  CUD operations should use this
     */
    public long getWriteShard( Id rowKeyId, UUID version, String... types );


    /**
     * Get the iterator of all shards for this entity
     */
    public Iterator<Long> getReadShards( Id rowKeyId, UUID maxVersion, String... types );


    /**
     * Get the name of the column family for getting source nodes
     */
    public String getSourceNodeCfName();

    /**
     * Get the name of the column family for getting target nodes
     */
    public String getTargetNodeCfName();


    /**
     * Get the name of the column family for getting source nodes  with a target type
     */
    public String getSourceNodeTargetTypeCfName();

    /**
     * Get the name of the column family for getting target nodes with a source type
     */
    public String getTargetNodeSourceTypeCfName();
}
