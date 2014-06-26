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
package org.apache.usergrid.persistence.graph.serialization.impl.shard.count;


import java.util.Arrays;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 * Key for shards and counts
 */
public class ShardKey {
    private final ApplicationScope scope;
    private final Id nodeId;
    private final long shardId;
    private final String[] edgeTypes;


    public ShardKey( final ApplicationScope scope, final Id nodeId, final long shardId, final String[] edgeTypes ) {


        this.scope = scope;
        this.nodeId = nodeId;
        this.shardId = shardId;
        this.edgeTypes = edgeTypes;
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }

        final ShardKey shardKey = ( ShardKey ) o;

        if ( shardId != shardKey.shardId ) {
            return false;
        }
        if ( !Arrays.equals( edgeTypes, shardKey.edgeTypes ) ) {
            return false;
        }
        if ( !nodeId.equals( shardKey.nodeId ) ) {
            return false;
        }
        if ( !scope.equals( shardKey.scope ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = scope.hashCode();
        result = 31 * result + nodeId.hashCode();
        result = 31 * result + ( int ) ( shardId ^ ( shardId >>> 32 ) );
        result = 31 * result + Arrays.hashCode( edgeTypes );
        return result;
    }
}
