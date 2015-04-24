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

package org.apache.usergrid.corepersistence.results;


import org.apache.usergrid.corepersistence.command.CommandBuilder;
import org.apache.usergrid.corepersistence.command.read.graph.ReadGraphCollectionCommand;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;

import com.google.common.base.Preconditions;


/**
 * Create The collection graph query executor
 */
public class CollectionGraphQueryExecutor extends AbstractGraphQueryExecutor {

    private final GraphManagerFactory graphManagerFactory;
    private final String collectionName;


    public CollectionGraphQueryExecutor( final EntityCollectionManagerFactory entityCollectionManagerFactory,
                                         final GraphManagerFactory graphManagerFactory,
                                         final ApplicationScope applicationScope, final EntityRef source,
                                         final String cursor, final String collectionName, final int limit ) {

        super( entityCollectionManagerFactory, applicationScope, source, cursor, limit );
        this.graphManagerFactory = graphManagerFactory;

        Preconditions.checkNotNull( collectionName, "collectionName is required on the query" );
        this.collectionName = collectionName;
    }


    @Override
    protected void addTraverseCommand( final CommandBuilder commandBuilder ) {
        //set the traverse command from the source Id to the connect name
        commandBuilder.withTraverseCommand( new ReadGraphCollectionCommand( graphManagerFactory, collectionName ) );
    }
}
