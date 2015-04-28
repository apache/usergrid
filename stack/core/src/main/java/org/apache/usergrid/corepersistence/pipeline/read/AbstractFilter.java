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


import java.io.Serializable;

import org.apache.usergrid.corepersistence.pipeline.cursor.CursorSerializer;
import org.apache.usergrid.corepersistence.pipeline.cursor.RequestCursor;
import org.apache.usergrid.corepersistence.pipeline.cursor.ResponseCursor;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;

import com.google.common.base.Optional;


/**
 * Basic functionality for our commands to handle cursor IO
 */
public abstract class AbstractFilter<T, C extends Serializable> implements Filter<T> {

    private int id;
    /**
     * The cache of the cursor that was set when the read was started
     */
    private RequestCursor readCache;

    /**
     * The current state of the write cache.  Gets updated as we traverse the observables
     */
    private ResponseCursor writeCache;


    /**
     * The applicationScope
     */
    protected ApplicationScope applicationScope;


    @Override
    public void setId( final int id ) {
        this.id = id;
    }


    @Override
    public void setCursorCaches( final RequestCursor readCache, final ResponseCursor writeCache ) {
        this.readCache = readCache;
        this.writeCache = writeCache;
    }


    @Override
    public void setApplicationScope( final ApplicationScope applicationScope ) {
       this.applicationScope = applicationScope;
    }


    /**
     * Return the parsed value of the cursor from the last request, if it exists
     */
    protected Optional<C> getCursor() {
        final C cursor = readCache.getCursor( id, getCursorSerializer() );

        return Optional.fromNullable( cursor );
    }





    /**
     * Set the cursor value into the new cursor write cache
     * @param newValue
     */
    protected void setCursor(final C newValue){
        writeCache.setCursor( id, newValue,  getCursorSerializer() );
    }


    /**
     * Generate our state as a cursor
     * @return
     */
    protected String generateCursor(){
        return writeCache.encodeAsString();
    }

    /**
     * Return the class to be used when parsing the cursor
     */
    protected abstract CursorSerializer<C> getCursorSerializer();

}
