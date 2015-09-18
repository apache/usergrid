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


import java.util.Iterator;
import java.util.UUID;

import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.rx.ObservableIterator;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdge;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;


/**
 * SimpleRepair operation
 */

public class EdgeDeleteRepairImpl implements EdgeDeleteRepair {


    private static final Logger LOG = LoggerFactory.getLogger( EdgeDeleteRepairImpl.class );

    protected final EdgeSerialization storageSerialization;
    protected final GraphFig graphFig;
    protected final Keyspace keyspace;


    @Inject
    public EdgeDeleteRepairImpl( final EdgeSerialization storageSerialization,
                                 final GraphFig graphFig, final Keyspace keyspace ) {

        Preconditions.checkNotNull( "storageSerialization is required", storageSerialization );
        Preconditions.checkNotNull( "consistencyFig is required", graphFig );
        Preconditions.checkNotNull( "keyspace is required", keyspace );


        this.storageSerialization = storageSerialization;
        this.graphFig = graphFig;
        this.keyspace = keyspace;
    }


    public Observable<MarkedEdge> repair( final ApplicationScope scope, final MarkedEdge edge, final UUID timestamp ) {


        //merge source and target then deal with the distinct values
        return Observable.just( edge ).filter( markedEdge-> markedEdge.isDeleted() )
                .doOnNext( markedEdge -> {
                    //it's still in the same state as it was when we queued it. Remove it
                        LOG.info( "Removing edge {} ", markedEdge );

                        //remove from the commit log


                        //remove from storage
                        try {
                            storageSerialization.deleteEdge( scope, markedEdge, timestamp ).execute();
                        }
                        catch ( ConnectionException e ) {
                            throw new RuntimeException( "Unable to connect to casandra", e );
                        }
                    }
              );
    }



}
