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


import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.Shard;


/**
 * Key for shards and counts
 */
public class ShardKey {
    public final ApplicationScope scope;
    public final Shard shard;
    public final DirectedEdgeMeta directedEdgeMeta;


    public ShardKey( final ApplicationScope scope, final Shard shard, final DirectedEdgeMeta directedEdgeMeta ) {
        this.scope = scope;
        this.shard = shard;
        this.directedEdgeMeta = directedEdgeMeta;
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

        if ( !directedEdgeMeta.equals( shardKey.directedEdgeMeta ) ) {
            return false;
        }
        if ( !scope.equals( shardKey.scope ) ) {
            return false;
        }
        if ( !shard.equals( shardKey.shard ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = scope.hashCode();
        result = 31 * result + shard.hashCode();
        result = 31 * result + directedEdgeMeta.hashCode();
        return result;
    }
}
