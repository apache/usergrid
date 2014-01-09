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


import java.util.UUID;

import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 * Simple bean to represent our edge
 * @author tnine
 */
public class SimpleEdge implements Edge {

    private final Id sourceNode;
    private final String type;
    private final Id targetNode;
    private final UUID version;


    public SimpleEdge( final Id sourceNode, final String type, final Id targetNode, final UUID version ) {

        ValidationUtils.verifyIdentity( sourceNode );
        ValidationUtils.verifyString( type, "type" );
        ValidationUtils.verifyIdentity( targetNode );
        ValidationUtils.verifyTimeUuid( version, "version" );
        this.sourceNode = sourceNode;
        this.type = type;
        this.targetNode = targetNode;
        this.version = version;
    }


    @Override
    public Id getSourceNode() {
        return sourceNode;
    }


    @Override
    public String getType() {
        return type;
    }


    @Override
    public Id getTargetNode() {
        return targetNode;
    }


    public UUID getVersion() {
        return version;
    }
}
