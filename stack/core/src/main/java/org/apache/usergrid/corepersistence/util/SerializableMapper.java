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

package org.apache.usergrid.corepersistence.util;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.impl.migration.EntityIdScope;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.google.common.base.Preconditions;


/**
 * A simple utility for serializing serializable classes to/and from strings.  To be used for small object storage only, such as resume on re-index
 * storing data such as entities should be specialized.
 */
public class SerializableMapper {

    private static final SmileFactory SMILE_FACTORY = new SmileFactory();

    private static final ObjectMapper MAPPER = new ObjectMapper( SMILE_FACTORY );

    static{
        MAPPER.enableDefaultTypingAsProperty( ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, "@class" );
        SMILE_FACTORY.delegateToTextual( true );
    }

    /**
     * Get value as a string
     * @param toSerialize
     * @param <T>
     * @return
     */
    public static <T extends Serializable> String asString(final T toSerialize){
        try {
            return MAPPER.writeValueAsString( toSerialize );
        }
        catch ( JsonProcessingException e ) {
            throw new RuntimeException( "Unable to process json", e );
        }
    }


    /**
     * Write the value as a string
     * @param <T>
     * @param serialized
     * @param clazz
     * @return
     */
    public static <T extends Serializable> T fromString(final String serialized, final Class<T> clazz){
        Preconditions.checkNotNull(serialized, "serialized string cannot be null");


        InputStream stream = new ByteArrayInputStream(serialized.getBytes( StandardCharsets.UTF_8));

        try {
            return MAPPER.readValue( stream, clazz );
        }
        catch ( IOException e ) {
            throw new RuntimeException( String.format("Unable to parse string '%s'", serialized), e );
        }
    }


}
