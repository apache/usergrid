/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.corepersistence.util;


import java.io.IOException;
import java.io.Serializable;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;


/**
 * An utility class to serialize and de-serialized objects as json strings
 */
public final class ObjectJsonSerializer {


    private final JsonFactory JSON_FACTORY = new JsonFactory();

    private final ObjectMapper MAPPER = new ObjectMapper( JSON_FACTORY );

    public ObjectJsonSerializer( ) {
        MAPPER.enableDefaultTypingAsProperty( ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, "@class" );
    }


    public <T extends Serializable> String toByteBuffer( final T toSerialize ) {

        Preconditions.checkNotNull( toSerialize, "toSerialize must not be null" );
        final String stringValue;
        //mark this version as empty

        //Convert to internal entity map
        try {
            stringValue = MAPPER.writeValueAsString( toSerialize );
        }
        catch ( JsonProcessingException jpe ) {
            throw new RuntimeException( "Unable to serialize entity", jpe );
        }

        return stringValue;
    }


    public <T extends Serializable> T fromString( final String value, final Class<T> toSerialize ) {

        Preconditions.checkNotNull( value, "value must not be null" );

        try {
            return MAPPER.readValue( value, toSerialize );
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Unable to deserialize", e );
        }
    }
}
