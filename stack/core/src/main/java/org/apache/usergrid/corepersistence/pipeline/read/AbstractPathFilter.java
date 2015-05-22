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

import com.google.common.base.Optional;


/**
 * Abstract class for filters to extend that require a cursor
 * @param <T> The input type
 * @param <R> The response type
 * @param <C> The cursor type
 */
public abstract class AbstractPathFilter<T, R, C extends Serializable> extends AbstractFilter<FilterResult<T>, FilterResult<R>>  {



    //TODO not a big fan of this, but not sure how to build resume otherwise
    private CursorSeek<C> cursorSeek;


    /**
     * Return the parsed value of the cursor from the last request, if it exists
     */
    protected Optional<C> getSeekValue() {

        if(cursorSeek == null) {
            final Optional<C> cursor = pipelineContext.getCursor( getCursorSerializer() );
            cursorSeek = new CursorSeek<>( cursor );
        }

        return cursorSeek.getSeekValue();

    }


    /**
     * Sets the cursor into our pipeline context
     */
    protected FilterResult<R> createFilterResult( final R emit, final C cursorValue, final Optional<EdgePath> parent ){


        //create a current path, and append our parent path to it
        final EdgePath<C> newEdgePath =
            new EdgePath<>( pipelineContext.getId(), cursorValue, getCursorSerializer(), parent );

        //emit our value with the parent path
        return new FilterResult<>( emit, Optional.of( newEdgePath ) );

    }


    /**
     * Return the class to be used when parsing the cursor
     */
    protected abstract CursorSerializer<C> getCursorSerializer();
}
