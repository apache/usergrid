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

package org.apache.usergrid.corepersistence.pipeline.cursor;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Interface for cursor serialization
 *
 * TODO, the need for this seems to indicate an issue with our object composition.  Refactor this away
 */
public class NoCursorSerializer<T> implements CursorSerializer<T> {

    private static final NoCursorSerializer<Object> INSTANCE = new NoCursorSerializer<>();


    @Override
    public T fromJsonNode( final JsonNode node, final ObjectMapper objectMapper ) {
        return null;
    }


    @Override
    public JsonNode toNode( final ObjectMapper objectMapper, final T value ) {
        return objectMapper.createObjectNode();
    }


    /**
     * convenience for type casting
     */
    public static <T> NoCursorSerializer<T> create() {
        return ( NoCursorSerializer<T> ) INSTANCE;
    }
}
