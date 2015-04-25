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


import org.apache.usergrid.corepersistence.pipeline.cursor.RequestCursor;
import org.apache.usergrid.corepersistence.pipeline.cursor.ResponseCursor;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;

import rx.Observable;


/**
 * Interface for filtering commands.  All filters must take an observable of Id's as an input.  Output is then determined by subclasses.
 * This takes an input of Id, performs some operation, and emits values for further processing in the Observable
 * pipeline
 */
public interface Filter<T> extends Observable.Transformer<Id, T> {


    /**
     * Set the id of this command in it's execution environment
     */
    void setId( final int id );

    /**
     * Set the cursor cache into the command
     *
     * @param readCache Set the cache that was used in the request
     * @param writeCache Set the cache to be used when writing the results
     */
    void setCursorCaches( final RequestCursor readCache, final ResponseCursor writeCache );

    /**
     * Set the application scope of the command
     * @param applicationScope
     */
    void setApplicationScope(final ApplicationScope applicationScope);
}
