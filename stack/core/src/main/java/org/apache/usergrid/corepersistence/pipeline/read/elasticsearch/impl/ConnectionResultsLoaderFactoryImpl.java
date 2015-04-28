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

package org.apache.usergrid.corepersistence.pipeline.read.elasticsearch.impl;


import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.ApplicationEntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.SearchEdge;


/**
 * Factory for creating results
 */
public class ConnectionResultsLoaderFactoryImpl implements ResultsLoaderFactory {

    private final EntityCollectionManager entityCollectionManager;
    private final ApplicationEntityIndex applicationEntityIndex;
    private final EntityRef ownerId;
    private final String connectionType;


    public ConnectionResultsLoaderFactoryImpl( final EntityCollectionManager entityCollectionManager,
                                               final ApplicationEntityIndex applicationEntityIndex, final EntityRef ownerId,
                                               final String connectionType ) {
        this.entityCollectionManager = entityCollectionManager;
        this.applicationEntityIndex = applicationEntityIndex;
        this.ownerId = ownerId;
        this.connectionType = connectionType;
    }


    @Override
    public ResultsLoader getLoader( final ApplicationScope applicationScope, final SearchEdge scope,
                                    final Query.Level resultsLevel ) {

        ResultsVerifier verifier;

        if ( resultsLevel == Query.Level.REFS ) {
            verifier = new ConnectionRefsVerifier( ownerId, connectionType );
        }
        else if ( resultsLevel == Query.Level.IDS ) {
            verifier = new ConnectionRefsVerifier( ownerId, connectionType );
            ;
        }
        else {
            verifier = new EntityVerifier( Query.MAX_LIMIT );
        }

        return new FilteringLoader( entityCollectionManager, applicationEntityIndex, verifier, applicationScope, scope );
    }
}
