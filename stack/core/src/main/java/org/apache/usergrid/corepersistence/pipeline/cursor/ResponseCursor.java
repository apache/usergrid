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


import java.io.Serializable;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


/**
 * A cursor used in rendering a response
 */
public class ResponseCursor {


    private static final ObjectMapper MAPPER = CursorSerializerUtil.getMapper();
    private static final Base64Variant VARIANT = CursorSerializerUtil.getBase64();

    /**
     * We use a map b/c some indexes might be skipped
     */
    private Map<Integer, CursorEntry<?>> cursors = new HashMap<>();


    /**
     * Set the possible cursor value into the index. DOES NOT parse the cursor.  This is intentional for performance
     */
    public <T extends Serializable> void setCursor( final int id, final T cursor,
                                                    final CursorSerializer<T> serializer ) {

        final CursorEntry<T> newEntry = new CursorEntry<>( cursor, serializer );
        cursors.put( id, newEntry );
    }


    /**
     * now we're done, encode as a string
     */
    public String encodeAsString() {
        try {
            final ObjectNode map = MAPPER.createObjectNode();

            for ( Map.Entry<Integer, CursorEntry<?>> entry : cursors.entrySet() ) {

                final CursorEntry cursorEntry = entry.getValue();

                final JsonNode serialized = cursorEntry.serializer.toNode( MAPPER, cursorEntry.cursor );

                map.put( entry.getKey().toString(), serialized );
            }


            final byte[] output = MAPPER.writeValueAsBytes(map);

            //generate a base64 url save string
            return Base64.getUrlEncoder().encodeToString( output );
//            return MAPPER.writer( VARIANT ).writeValueAsString( map );

        }
        catch ( JsonProcessingException e ) {
            throw new CursorParseException( "Unable to serialize cursor", e );
        }
    }


    /**
     * Interal pointer to the cursor and it's serialzed value
     */
    private static final class CursorEntry<T> {
        private final T cursor;
        private final CursorSerializer<T> serializer;


        private CursorEntry( final T cursor, final CursorSerializer<T> serializer ) {
            this.cursor = cursor;
            this.serializer = serializer;
        }
    }
}
