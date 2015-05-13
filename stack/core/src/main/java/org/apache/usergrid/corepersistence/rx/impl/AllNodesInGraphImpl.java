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

package org.apache.usergrid.corepersistence.rx.impl;


import org.apache.usergrid.persistence.collection.serialization.impl.migration.EntityIdScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.serialization.TargetIdObservable;
import org.apache.usergrid.persistence.graph.serialization.impl.migration.GraphNode;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * An observable that will emit every entity's node Id stored in our entire system across all apps.
 * Note that this only walks each application applicationId graph, and emits edges from the applicationId and it's edges as the s
 * source node
 */
@Singleton
public class AllNodesInGraphImpl extends AbstractGraphVisitorImpl<GraphNode> {


    @Inject
    public AllNodesInGraphImpl( final AllApplicationsObservable applicationObservable,
                                final AllEntityIdsObservable allEntityIdsObservable ) {
        super( applicationObservable, allEntityIdsObservable );
    }


    @Override
    protected GraphNode generateData( final EntityIdScope entityIdScope ) {
        return new GraphNode(entityIdScope.getApplicationScope(), entityIdScope.getId());
    }
}
