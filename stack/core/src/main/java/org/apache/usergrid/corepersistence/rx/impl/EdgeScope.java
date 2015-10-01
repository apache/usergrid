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


import java.io.Serializable;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;


/**
 * Get the scope for the edge
 */
public final class EdgeScope implements Serializable {
    private final ApplicationScope applicationScope;
    private final Edge edge;


    public EdgeScope( final ApplicationScope applicationScope, final Edge edge ) {
        this.applicationScope = applicationScope;
        this.edge = edge;
    }


    public ApplicationScope getApplicationScope() {
        return applicationScope;
    }


    public Edge getEdge() {
        return edge;
    }
}
