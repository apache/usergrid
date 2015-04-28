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
public abstract class AbstractSeekingFilter<T, R, C extends Serializable> extends AbstractPipelineOperation<T, R> implements Filter<T, R> {



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
     * @param newValue
     */
    protected void setCursor(final C newValue){
        pipelineContext.setCursorValue( newValue, getCursorSerializer() );
    }


    /**
     * Return the class to be used when parsing the cursor
     */
    protected abstract CursorSerializer<C> getCursorSerializer();


    /**
     * An internal class that holds a mutable state.  When resuming, we only ever honor the seek value on the first call.  Afterwards, we will seek from the beginning on newly emitted values.
     * Calling get will return the first value to seek, or absent if not specified.  Subsequent calls will return absent.  Callers should treat the results as seek values for each operation
     */
    protected static class CursorSeek<C> {

        private Optional<C> seek;

        private CursorSeek(final Optional<C> cursorValue){
            seek = cursorValue;
        }


        /**
         * Get the seek value to use when searching
         * @return
         */
        public Optional<C> getSeekValue(){
            final Optional<C> toReturn = seek;

            seek = Optional.absent();

            return toReturn;
        }



    }
}
