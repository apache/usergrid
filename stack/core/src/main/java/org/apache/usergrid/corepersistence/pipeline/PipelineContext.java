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

package org.apache.usergrid.corepersistence.pipeline;


import java.io.Serializable;

import org.apache.usergrid.corepersistence.pipeline.cursor.CursorSerializer;
import org.apache.usergrid.corepersistence.pipeline.cursor.RequestCursor;
import org.apache.usergrid.corepersistence.pipeline.cursor.ResponseCursor;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;

import com.google.common.base.Optional;
import org.apache.usergrid.persistence.index.query.ParsedQuery;
import org.apache.usergrid.persistence.index.query.ParsedQueryBuilder;

import static org.apache.usergrid.persistence.Query.MAX_LIMIT;


/**
 * Encapsulates the context of the pipeline for the scope of the filter.
 */
public class PipelineContext {

    private final int id;
    private final ApplicationScope applicationScope;
    private final RequestCursor requestCursor;
    private final int limit;
    // An entry is stale if the ES version number is less than the Cassandra version number
    // it can happen if ES was not updated or has yet to be updated.
    private final boolean keepStaleEntries;
    private String query;
    private ParsedQuery parsedQuery;


    public PipelineContext( final ApplicationScope applicationScope, final RequestCursor requestCursor, final int limit, final int id, boolean keepStaleEntries, String query ) {

        this.applicationScope = applicationScope;
        this.id = id;
        this.keepStaleEntries = keepStaleEntries;
        this.query = query;
        this.parsedQuery = ParsedQueryBuilder.build(query);
        if (parsedQuery != null && parsedQuery.isDirectQuery()) {
            // for direct query, use no limit or cursor
            this.limit = MAX_LIMIT + 1;
            this.requestCursor = new RequestCursor(Optional.absent());
        } else {
            this.limit = limit;
            this.requestCursor = requestCursor;
        }
    }


    public ApplicationScope getApplicationScope() {
        return applicationScope;
    }


    public int getId() {
        return id;
    }


    /**
     * Get our cursor value if present from our pipline
     * @param serializer
     */
    public <T extends Serializable> Optional<T> getCursor( final CursorSerializer<T> serializer ) {
        final T value = requestCursor.getCursor( id, serializer );

        return Optional.fromNullable( value );
    }

    /**
     * Get the limit for this execution
     * @return
     */
    public int getLimit() {
        return limit;
    }

    /**
     * return true if stales entries are not to be filtered out.
     */
    public boolean getKeepStaleEntries() {
        return keepStaleEntries;
    }

    /**
     * return the query string if any
     */
    public String getQuery() {
        return query;
    }

    public ParsedQuery getParsedQuery() {
        return parsedQuery;
    }

}
