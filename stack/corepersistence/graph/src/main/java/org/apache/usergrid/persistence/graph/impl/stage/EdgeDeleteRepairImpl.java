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

package org.apache.usergrid.persistence.graph.impl.stage;


import java.util.UUID;

import org.apache.usergrid.persistence.collection.OrganizationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;

import com.fasterxml.uuid.UUIDComparator;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;


/**
 * SimpleRepair operation
 */
@Singleton
public class EdgeDeleteRepairImpl extends AbstractEdgeRepair implements EdgeDeleteRepair {


    @Inject
    public EdgeDeleteRepairImpl( final EdgeSerialization edgeSerialization, final GraphFig graphFig,
                                 final Keyspace keyspace) {
        super( edgeSerialization, graphFig, keyspace );
    }


    @Override
    public Observable<MarkedEdge> repair( final OrganizationScope scope, final Edge edge ) {

        return super.repair( scope, edge );
    }


    @Override
    protected Func1<MarkedEdge, Boolean> getFilter( final UUID maxVersion ) {
        return new Func1<MarkedEdge, Boolean>() {
            /**
             * We only want to return edges < this version so we remove them
             * @param markedEdge
             * @return
             */
            @Override
            public Boolean call( final MarkedEdge markedEdge ) {
                return UUIDComparator.staticCompare( markedEdge.getVersion(), maxVersion ) < 1;
            }
        };
    }
}
