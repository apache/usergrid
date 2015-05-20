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


import org.apache.usergrid.corepersistence.pipeline.read.EdgePath;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Optional;


/**
 * A cursor used in rendering a response
 */
public class ResponseCursor {


    private static final ObjectMapper MAPPER = CursorSerializerUtil.getMapper();


    /**
     * The pointer to the first edge path.  Evaluation is lazily performed in the case the caller does not care about
     * the cursor.
     */
    private final Optional<EdgePath> edgePath;

    private Optional<String> encodedValue = null;


    public ResponseCursor( final Optional<EdgePath> edgePath ) {this.edgePath = edgePath;}


    /**
     * Lazyily encoded deliberately.  If the user doesn't care about a cursor and is using streams, we dont' want to
     * take the time to calculate it
     */
    public Optional<String> encodeAsString() {

        //always return cached if we are called 2x
        if ( encodedValue != null ) {
            return encodedValue;
        }

        if ( !edgePath.isPresent() ) {
            encodedValue = Optional.absent();
            return encodedValue;
        }

        //no edge path, short circuit

        final ObjectNode map = MAPPER.createObjectNode();


        Optional<EdgePath> current = edgePath;


        //traverse each edge and add them to our json
        do {

            final EdgePath edgePath = current.get();
            final Object cursorValue = edgePath.getCursorValue();
            final CursorSerializer serializer = edgePath.getSerializer();
            final int filterId = edgePath.getFilterId();

            final JsonNode serialized = serializer.toNode( MAPPER, cursorValue );
            map.put( String.valueOf( filterId ), serialized );

            current = current.get().getPrevious();
        }
        while ( current.isPresent() );

        //generate a base64 url save string
        final String value = CursorSerializerUtil.asString( map );

        encodedValue = Optional.of( value );


        return encodedValue;
    }
}
