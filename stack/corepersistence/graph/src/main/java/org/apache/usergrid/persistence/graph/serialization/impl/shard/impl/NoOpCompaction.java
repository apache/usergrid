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


import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.DirectedEdgeMeta;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardEntryGroup;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardGroupCompaction;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;


/**
 * Class that just ignores compaction events, since we're already evaluating the events.  A bit of a hack that shows we
 * need some refactoring
 */
public class NoOpCompaction implements ShardGroupCompaction {

    @Override
    public ListenableFuture<AuditResult> evaluateShardGroup( final ApplicationScope scope,
                                                             final DirectedEdgeMeta edgeMeta,
                                                             final ShardEntryGroup group ) {

        //deliberately a no op
        return Futures.immediateFuture( AuditResult.NOT_CHECKED );
    }
}
