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


import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;


/**
 * A cursor that has been passed in with our request.  Adds utils for parsing values
 */
public class RequestCursor {


    private static final int MAX_CURSOR_COUNT = 100;

    private static final ObjectMapper MAPPER = CursorSerializerUtil.getMapper();

    private final Map<Integer, JsonNode> parsedCursor;


    public RequestCursor( final Optional<String> cursor ) {
        if ( cursor.isPresent() ) {
            parsedCursor = fromCursor( cursor.get() );
        }
        else {
            parsedCursor = Collections.EMPTY_MAP;
        }
    }


    /**
     * Get the cursor with the specified id
     *
     * May return null if not found
     */
    public <T> T getCursor( final int id, final CursorSerializer<T> serializer ) {

        final JsonNode node = parsedCursor.get( id );

        if(node == null){
            return null;
        }

        return serializer.fromJsonNode( node, MAPPER );
    }


    /**
     * Deserialize from the cursor as json nodes
     */
    private Map<Integer, JsonNode> fromCursor( final String cursor ) throws CursorParseException {
        try {



            JsonNode jsonNode = CursorSerializerUtil.fromString( cursor );


            Preconditions
                .checkArgument( jsonNode.size() <= MAX_CURSOR_COUNT, " You cannot have more than " + MAX_CURSOR_COUNT + " cursors" );


            Map<Integer, JsonNode> cursors = new HashMap<>();

            final Iterable<Map.Entry<String, JsonNode>> iterable = () -> jsonNode.fields();

            for ( final Map.Entry<String, JsonNode> node : iterable ) {
                cursors.put( Integer.parseInt( node.getKey() ), node.getValue() );
            }

            return cursors;
        }
        catch ( Exception e ) {
            throw new CursorParseException( "Unable to serialize cursor", e );
        }
    }
}
