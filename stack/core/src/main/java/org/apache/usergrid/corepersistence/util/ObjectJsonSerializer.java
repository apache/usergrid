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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;


/**
 * An utility class to serialize and de-serialized objects as json strings
 */
public final class ObjectJsonSerializer {


    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    private static final ObjectMapper MAPPER = new ObjectMapper( JSON_FACTORY );

    static{

           /**
            * Because of the way SNS escapes all our json, we have to tell jackson to accept it.  See the documentation
            * here for how SNS borks the message body
            *
            *  http://docs.aws.amazon.com/sns/latest/dg/SendMessageToHttp.html
            */
            MAPPER.configure( JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true );
       }

    /**
     * Singleton instance of our serializer, instantiating it and configuring the mapper is expensive.
     */
    public static final ObjectJsonSerializer INSTANCE = new ObjectJsonSerializer();


    private ObjectJsonSerializer( ) {

    }


    public <T extends Serializable> String toString( final T toSerialize ) {

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
