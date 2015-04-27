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

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;


/**
 * A utility to serialize objects to/from cursors
 */
public class CursorSerializer {


    private static final SmileFactory SMILE_FACTORY = new SmileFactory();

    private static final ObjectMapper MAPPER = new ObjectMapper( SMILE_FACTORY );

    private static final Base64Variant VARIANT = Base64Variants.MODIFIED_FOR_URL;


    /**
     * Serialize the serializable object as a cursor
     */
    public static String asCursor( final Serializable cursor ) {

        try {
            return MAPPER.writer( VARIANT ).writeValueAsString( cursor );
        }
        catch ( JsonProcessingException e ) {
            throw new CursorParseException( "Unable to serialize cursor", e );
        }
    }


    /**
     * Deserialize from the cursor
     * @param cursor
     * @return
     * @throws CursorParseException
     */
    public <T extends Serializable> T fromCursor( final String cursor, final Class<T> cursorClass ) throws CursorParseException {
        try {

            final JsonParser parser = MAPPER.getFactory().createParser( cursor );
            return MAPPER.reader( VARIANT ).readValue( parser, cursorClass);
        }
        catch ( Exception e ) {
            throw new CursorParseException( "Unable to serialize cursor", e );
        }
    }


    /**
     * Thrown when we can't parse a cursor
     */
    public static class CursorParseException extends RuntimeException {
        public CursorParseException( final String message, final Throwable cause ) {
            super( message, cause );
        }
    }
}
