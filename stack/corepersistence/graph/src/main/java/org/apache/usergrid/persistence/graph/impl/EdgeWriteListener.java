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
package org.apache.usergrid.persistence.graph.impl;


import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.usergrid.persistence.core.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.core.consistency.MessageListener;
import org.apache.usergrid.persistence.core.rx.ObservableIterator;
import org.apache.usergrid.persistence.core.scope.OrganizationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.guice.CommitLogEdgeSerialization;
import org.apache.usergrid.persistence.graph.guice.EdgeWrite;
import org.apache.usergrid.persistence.graph.guice.StorageEdgeSerialization;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeWriteCompact;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.fasterxml.uuid.UUIDComparator;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;
import rx.functions.Func1;


/**
 * Construct the asynchronous delete operation from the listener.  This really shouldn't exist in graph per se, but
 * rather as a part of the usergrid mechanism
 */
@Singleton
public class EdgeWriteListener implements MessageListener<EdgeEvent<MarkedEdge>, Integer> {


    private final EdgeWriteCompact edgeWriteCompact;


    @Inject
    public EdgeWriteListener( final EdgeWriteCompact edgeWriteCompact, @EdgeWrite final AsyncProcessor<EdgeEvent<MarkedEdge>> edgeWrite) {


        Preconditions.checkNotNull( edgeWriteCompact, "edgeWriteCompact is required" );
        Preconditions.checkNotNull( edgeWrite, "edgeWrite is required" );


        this.edgeWriteCompact = edgeWriteCompact;

        edgeWrite.addListener( this );
    }


    @Override
    public Observable<Integer> receive( final EdgeEvent<MarkedEdge> write ) {
       return edgeWriteCompact.compact( write.getOrganizationScope(), write.getData(), write.getTimestamp() );
    }
}
