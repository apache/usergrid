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

package org.apache.usergrid.persistence.graph.serialization.impl.shard.impl;


import org.apache.usergrid.persistence.graph.GraphFig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.model.ConsistencyLevel;


@Singleton
public class ShardConsistency {


    public final GraphFig graphFig;

    @Inject
    public ShardConsistency( final GraphFig graphFig ) {this.graphFig = graphFig;}


    public ConsistencyLevel getShardWriteConsistency() {
        return ConsistencyLevel.valueOf( this.graphFig.getShardWriteConsistency() );
    }


    public ConsistencyLevel getShardReadConsistency() {
        return ConsistencyLevel.valueOf( this.graphFig.getShardReadConsistency() );
    }
}
