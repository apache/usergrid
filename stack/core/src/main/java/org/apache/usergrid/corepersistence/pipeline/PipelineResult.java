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


import org.apache.usergrid.corepersistence.pipeline.cursor.ResponseCursor;

import com.google.common.base.Optional;


/**
 * Intermediate observable that will return results, as well as an optional cursor
 * @param <R>
 */
public class PipelineResult<R> {


    private final R result;

    private final ResponseCursor responseCursor;


    public PipelineResult( final R result, final ResponseCursor responseCursor ) {
        this.result = result;
        this.responseCursor = responseCursor;
    }


    /**
     * If the user requests our cursor, return the cursor
     * @return
     */
    public Optional<String> getCursor(){
        return this.responseCursor.encodeAsString();
    }

    public R getResult(){
        return result;
    }
}
