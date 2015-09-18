/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */
package org.apache.usergrid.persistence.graph.serialization.impl;


import com.google.inject.Inject;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.serialization.EdgesObservable;
import org.apache.usergrid.persistence.graph.serialization.TargetIdObservable;
import org.apache.usergrid.persistence.model.entity.Id;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Func1;

/**
 * Emits the id of all nodes that are target nodes from the given source node
 */
public class TargetIdObservableImpl implements TargetIdObservable {

    private static final Logger logger = LoggerFactory.getLogger(TargetIdObservableImpl.class);
    private final EdgesObservable edgesFromSourceObservable;

    @Inject
    public TargetIdObservableImpl(final EdgesObservable edgesFromSourceObservable){
        this.edgesFromSourceObservable = edgesFromSourceObservable;
    }

    /**
     * Get all nodes that are target nodes from the sourceNode
     * @param gm
     * @param sourceNode
     *
     * @return
     */
    @Override
    public Observable<Id> getTargetNodes(final GraphManager gm, final Id sourceNode) {

        //only search edge types that start with collections
        return edgesFromSourceObservable.edgesFromSourceDescending( gm, sourceNode ).map( edge -> {
            final Id targetNode = edge.getTargetNode();

            logger.debug( "Emitting targetId of {}", edge );


            return targetNode;
        } );
    }
}
