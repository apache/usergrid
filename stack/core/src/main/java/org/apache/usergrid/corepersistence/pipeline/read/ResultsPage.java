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


import java.util.List;

import org.apache.usergrid.corepersistence.pipeline.cursor.ResponseCursor;
import org.apache.usergrid.persistence.model.entity.Entity;


/**
 * An encapsulation of entities as a group of responses.  Ordered by the requesting filters.  Each set should be
 * considered a "page" of results.  A hold over from 1.0.  We shouldn't need this when we fully move away from the EM/RM
 *
 * @param T the type of results page
 */
public class ResultsPage<T> {

    private final List<T> entityList;

    private final int limit;

    private final ResponseCursor responseCursor;


    public ResultsPage( final List<T> entityList, final ResponseCursor responseCursor, final int limit ) {
        this.entityList = entityList;
        this.responseCursor = responseCursor;
        this.limit = limit;
    }


    public List<T> getEntityList() {
        return entityList;
    }


    /**
     * Return true if the results page is empty
     */
    public boolean hasMoreResults() {
        return entityList != null && entityList.size() == limit;
    }




    public ResponseCursor getResponseCursor() {
        return responseCursor;
    }
}
