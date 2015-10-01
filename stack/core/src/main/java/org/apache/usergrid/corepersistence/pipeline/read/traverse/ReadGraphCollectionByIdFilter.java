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

package org.apache.usergrid.corepersistence.pipeline.read.traverse;


import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;


/**
 * Read an edge in the graph to verify it's existence by id
 */
public class ReadGraphCollectionByIdFilter extends AbstractReadGraphEdgeByIdFilter{

    private final String collectionName;

    @Inject
    public ReadGraphCollectionByIdFilter( final GraphManagerFactory graphManagerFactory, @Assisted final String collectionName, @Assisted final Id targetId ) {
        super( graphManagerFactory, targetId );
        this.collectionName = collectionName;
    }


    @Override
    protected String getEdgeName() {
        return CpNamingUtils.getEdgeTypeFromCollectionName( collectionName );
    }
}
