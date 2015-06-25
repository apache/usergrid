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
package org.apache.usergrid.utils;


import java.io.File;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.exception.JsonReadException;
import org.apache.usergrid.exception.JsonWriteException;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.index.query.Identifier;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;

import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.apache.usergrid.utils.StringUtils.stringOrSubstringBeforeFirst;


public class JsonUtils {

    private static final Logger LOG = LoggerFactory.getLogger( JsonUtils.class );

    static ObjectMapper mapper = new ObjectMapper();

    static SmileFactory smile = new SmileFactory();

    static ObjectMapper smileMapper = new ObjectMapper( smile );

    private static ObjectMapper indentObjectMapper = new ObjectMapper();

    private static final Pattern UUID_PATTERN = Pattern.compile( Identifier.UUID_REX );


    static {
        //indentObjectMapper.getSerializationConfig().set( Feature.INDENT_OUTPUT, true );
        indentObjectMapper.getSerializationConfig().with( SerializationFeature.INDENT_OUTPUT );
    }


    /** Converts object to JSON string, throws runtime exception JsonWriteException on failure. */
    public static String mapToJsonString( Object obj ) {
        try {
            return mapper.writeValueAsString( obj );
        }
        catch ( Throwable t ) {
            LOG.debug( "Error generating JSON", t );
            throw new JsonWriteException( "Error generating JSON", t );
        }
    }


    /** Converts object to JSON string, throws runtime exception JsonWriteException on failure. */
    public static String mapToFormattedJsonString( Object obj ) {
        try {
            return indentObjectMapper.writeValueAsString( obj );
        }
        catch ( Throwable t ) {
            LOG.debug( "Error generating JSON", t );
            throw new JsonWriteException( "Error generating JSON", t );
        }
    }


    /** Parses JSON string  and returns object, throws runtime exception JsonReadException on failure. */
    public static Object parse( String json ) {
        try {
            return mapper.readValue( json, Object.class );
        }
        catch ( Throwable t ) {
            LOG.debug( "Error parsing JSON", t );
            throw new JsonReadException( "Error parsing JSON", t );
        }
    }


    public static String quoteString( String s ) {
        JsonStringEncoder encoder = new JsonStringEncoder();
        return new String( encoder.quoteAsUTF8( s ) );
    }


    public static ByteBuffer toByteBuffer( Object obj ) {
        if ( obj == null ) {
            return null;
        }

        byte[] bytes = null;
        try {
            bytes = smileMapper.writeValueAsBytes( obj );
        }
        catch ( Exception e ) {
            LOG.error( "Error getting SMILE bytes", e );
        }
        if ( bytes != null ) {
            return ByteBuffer.wrap( bytes );
        }
        return null;
    }


    public static Object fromByteBuffer( ByteBuffer byteBuffer ) {
        return fromByteBuffer( byteBuffer, Object.class );
    }


    public static Object fromByteBuffer( ByteBuffer byteBuffer, Class<?> clazz ) {
        if ( ( byteBuffer == null ) || !byteBuffer.hasRemaining() ) {
            return null;
        }
        if ( clazz == null ) {
            clazz = Object.class;
        }

        Object obj = null;
        try {
            obj = smileMapper.readValue( byteBuffer.array(), byteBuffer.arrayOffset() + byteBuffer.position(),
                    byteBuffer.remaining(), clazz );
        }
        catch ( Exception e ) {
            LOG.error( "Error parsing SMILE bytes", e );
        }
        return obj;
    }


    public static JsonNode toJsonNode( Object obj ) {
        if ( obj == null ) {
            return null;
        }
        return mapper.convertValue( obj, JsonNode.class );
    }


    @SuppressWarnings("unchecked")
    public static Map<String, Object> toJsonMap( Object obj ) {
        if ( obj == null ) {
            return null;
        }

        return ( Map<String, Object> ) mapper.convertValue( obj, Map.class );
    }


    private static UUID tryConvertToUUID( Object o ) {
        if ( o instanceof String ) {
            String s = ( String ) o;
            if ( UUID_PATTERN.matcher( s ).matches() ) {
                try {
                    return UUID.fromString( s );
                }
                catch ( IllegalArgumentException e ) {
                    LOG.warn( "Argument to UUID.fromString({}) was invalid.", s, e );
                }
            }
        }
        return null;
    }


    public static Object normalizeJsonTree( Object obj ) {
        if ( obj instanceof Map ) {
            @SuppressWarnings("unchecked") Map<Object, Object> m = ( Map<Object, Object> ) obj;
            Object o;
            UUID uuid;
            for ( Object k : m.keySet() ) {
                if ( k instanceof String && ( ( String ) k ).equalsIgnoreCase( "name" ) ) {
                    continue;
                }

                o = m.get( k );
                uuid = tryConvertToUUID( o );
                if ( uuid != null ) {
                    m.put( k, uuid );
                }
                else if ( o instanceof Integer ) {
                    m.put( k, ( ( Integer ) o ).longValue() );
                }
                else if ( o instanceof BigInteger ) {
                    m.put( k, ( ( BigInteger ) o ).longValue() );
                }
            }
        }
        else if ( obj instanceof List ) {
            @SuppressWarnings("unchecked") List<Object> l = ( List<Object> ) obj;
            Object o;
            UUID uuid;
            for ( int i = 0; i < l.size(); i++ ) {
                o = l.get( i );
                uuid = tryConvertToUUID( o );
                if ( uuid != null ) {
                    l.set( i, uuid );
                }
                else if ( ( o instanceof Map ) || ( o instanceof List ) ) {
                    normalizeJsonTree( o );
                }
                else if ( o instanceof Integer ) {
                    l.set( i, ( ( Integer ) o ).longValue() );
                }
                else if ( o instanceof BigInteger ) {
                    l.set( i, ( ( BigInteger ) o ).longValue() );
                }
            }
        }
        else if ( obj instanceof String ) {
            UUID uuid = tryConvertToUUID( obj );
            if ( uuid != null ) {
                return uuid;
            }
        }
        else if ( obj instanceof Integer ) {
            return ( ( Integer ) obj ).longValue();
        }
        else if ( obj instanceof BigInteger ) {
            return ( ( BigInteger ) obj ).longValue();
        }
        else if ( obj instanceof JsonNode ) {
            return mapper.convertValue( obj, Object.class );
        }
        return obj;
    }


    public static Object select( Object obj, String path ) {
        return select( obj, path, false );
    }


    public static Object select( Object obj, String path, boolean buildResultTree ) {

        if ( obj == null ) {
            return null;
        }

        if ( org.apache.commons.lang.StringUtils.isBlank( path ) ) {
            return obj;
        }

        String segment = stringOrSubstringBeforeFirst( path, '.' );
        String remaining = substringAfter( path, "." );

        if ( obj instanceof Map ) {
            Map<?, ?> map = ( Map<?, ?> ) obj;
            Object child = map.get( segment );
            Object result = select( child, remaining, buildResultTree );
            if ( result != null ) {
                if ( buildResultTree ) {
                    Map<Object, Object> results = new LinkedHashMap<Object, Object>();
                    results.put( segment, result );
                    return results;
                }
                else {
                    return result;
                }
            }
            return null;
        }
        if ( obj instanceof List ) {
            List<Object> results = new ArrayList<Object>();
            List<?> list = ( List<?> ) obj;
            for ( Object i : list ) {
                Object result = select( i, path, buildResultTree );
                if ( result != null ) {
                    results.add( result );
                }
            }
            if ( !results.isEmpty() ) {
                return results;
            }
            return null;
        }

        if ( obj instanceof Entity ) {
            Object child = ( ( Entity ) obj ).getProperty( segment );
            Object result = select( child, remaining, buildResultTree );
            if ( result != null ) {
                if ( buildResultTree ) {
                    Map<Object, Object> results = new LinkedHashMap<Object, Object>();
                    results.put( segment, result );
                    return results;
                }
                else {
                    return result;
                }
            }
            else {
                return result;
            }
        }

        return obj;
    }


    public static Object loadFromFilesystem( String filename ) {
        Object json = null;
        try {
            File file = new File( filename );
            json = mapper.readValue( file, Object.class );
        }
        catch ( Exception e ) {
            LOG.error( "Error loading JSON", e );
        }
        return json;
    }
}
