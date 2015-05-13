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

package org.apache.usergrid.corepersistence.pipeline.read;


import org.apache.usergrid.corepersistence.pipeline.cursor.CursorSerializer;

import com.google.common.base.Optional;


/**
 * A path from our input element to our emitted element.  A list of EdgePaths comprise a path through the graph.  The chains of edge paths will result
 * in a cursor when aggregated.  If a graph traversal is the following
 *
 * applicationId(1) - "users" -> userId(2) - "devices" -> deviceId(3).  There would be 2 EdgePath
 *
 *  EdgePath("users"->userId(2)) <- parent - EdgePath("devices" -> deviceId(3))
 */
public class EdgePath<C> {


    private final int filterId;
    private final C cursorValue;
    private final CursorSerializer<C> serializer;
    private final Optional<EdgePath> previous;


    /**
     *
     * @param filterId The id of the filter that generated this path
     * @param cursorValue The value to resume seeking on the path
     * @param serializer The serializer to serialize the value
     * @param parent The parent graph path edge to reach this path
     */
    public EdgePath( final int filterId, final C cursorValue, final CursorSerializer<C> serializer,
                     final Optional<EdgePath> parent ) {
        this.filterId = filterId;
        this.cursorValue = cursorValue;
        this.serializer = serializer;
        this.previous = parent;
    }


    public C getCursorValue() {
        return cursorValue;
    }


    public int getFilterId() {
        return filterId;
    }


    public Optional<EdgePath> getPrevious() {
        return previous;
    }


    public CursorSerializer<C> getSerializer() {
        return serializer;
    }
}
