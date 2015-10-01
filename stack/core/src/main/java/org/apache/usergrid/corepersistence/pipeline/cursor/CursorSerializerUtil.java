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


import java.io.IOException;
import java.util.Base64;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.google.common.base.Preconditions;


/**
 * A utility to serialize objects to/from cursors
 */
public class CursorSerializerUtil {

    private static final SmileFactory SMILE_FACTORY = new SmileFactory();

    private static final ObjectMapper MAPPER = new ObjectMapper( SMILE_FACTORY );

    /**
     * Aritrary number, just meant to keep us from having a DOS issue
     */
    private static final int MAX_SIZE = 1024;


    public static ObjectMapper getMapper() {
        return MAPPER;
    }


    /**
     * Turn the json node in to a base64 encoded SMILE binary
     */
    public static String asString( final JsonNode node ) {
        final byte[] output;
        try {
            output = MAPPER.writeValueAsBytes( node );
        }
        catch ( JsonProcessingException e ) {
            throw new RuntimeException( "Unable to create output from json node " + node );
        }

        //generate a base64 url save string
        final String value = Base64.getUrlEncoder().encodeToString( output );

        return value;
    }


    /**
     * Parse the base64 encoded binary string
     */
    public static JsonNode fromString( final String base64EncodedJson ) {

        Preconditions.checkArgument( base64EncodedJson.length() <= MAX_SIZE,
            "Your cursor must be less than " + MAX_SIZE + " chars in length" );

        final byte[] data = Base64.getUrlDecoder().decode( base64EncodedJson );

        JsonNode jsonNode;
        try {
            jsonNode =  MAPPER.readTree( data );
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Unable to parse json node from string " + base64EncodedJson );
        }

        return jsonNode;
    }
}
