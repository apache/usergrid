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
package org.apache.usergrid.persistence.index.utils;


import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang.math.NumberUtils;


/** Convenience methods for converting to and from formats, primarily between byte arrays and UUIDs, Strings,
 * and Longs. */
public class ConversionUtils {

    private static final Logger logger = LoggerFactory.getLogger( ConversionUtils.class );

    /**
     *
     */
    public static final String UTF8_ENCODING = "UTF-8";

    /**
     *
     */
    public static final String ASCII_ENCODING = "US-ASCII";

    public static final ByteBuffer HOLDER = ByteBuffer.wrap( new byte[] { 0 } );


    /**
     * @param uuid
     * @return
     */
    public static UUID uuid( byte[] uuid ) {
        return uuid( uuid, 0 );
    }


    /**
     * @param uuid
     * @param offset
     * @return
     */
    public static UUID uuid( byte[] uuid, int offset ) {
        ByteBuffer bb = ByteBuffer.wrap( uuid, offset, 16 );
        return new UUID( bb.getLong(), bb.getLong() );
    }


    public static UUID uuid( ByteBuffer bb ) {
        if ( bb == null ) {
            return null;
        }
        if ( bb.remaining() < 16 ) {
            return null;
        }
        bb = bb.slice();
        return new UUID( bb.getLong(), bb.getLong() );
    }


    /**
     * @param uuid
     * @return
     */
    public static UUID uuid( String uuid ) {
        try {
            return UUID.fromString( uuid );
        }
        catch ( Exception e ) {
            logger.error( "Bad UUID", e );
        }
        return UUIDUtils.ZERO_UUID;
    }


    /**
     * @param obj
     * @return
     */
    public static UUID uuid( Object obj ) {
        return uuid( obj, UUIDUtils.ZERO_UUID );
    }


    public static UUID uuid( Object obj, UUID defaultValue ) {
        if ( obj instanceof UUID ) {
            return ( UUID ) obj;
        }
        else if ( obj instanceof byte[] ) {
            return uuid( ( byte[] ) obj );
        }
        else if ( obj instanceof ByteBuffer ) {
            return uuid( ( ByteBuffer ) obj );
        }
        else if ( obj instanceof String ) {
            return uuid( ( String ) obj );
        }
        return defaultValue;
    }


    /**
     * @param uuid
     * @return
     */
    public static byte[] bytes( UUID uuid ) {
        if ( uuid == null ) {
            return null;
        }
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        byte[] buffer = new byte[16];

        for ( int i = 0; i < 8; i++ ) {
            buffer[i] = ( byte ) ( msb >>> ( 8 * ( 7 - i ) ) );
        }
        for ( int i = 8; i < 16; i++ ) {
            buffer[i] = ( byte ) ( lsb >>> ( 8 * ( 7 - i ) ) );
        }

        return buffer;
    }


    public static ByteBuffer bytebuffer( UUID uuid ) {
        if ( uuid == null ) {
            return null;
        }
        return ByteBuffer.wrap( bytes( uuid ) );
    }


    /**
     * @param uuid
     * @return
     */
    public static byte[] uuidToBytesNullOk( UUID uuid ) {
        if ( uuid != null ) {
            return bytes( uuid );
        }
        return new byte[16];
    }


    /**
     * @param s
     * @return
     */
    public static byte[] bytes( String s ) {
        return bytes( s, UTF8_ENCODING );
    }


    public static ByteBuffer bytebuffer( String s ) {
        return ByteBuffer.wrap( bytes( s ) );
    }


    /**
     * @param s
     * @return
     */
    public static byte[] ascii( String s ) {
        if ( s == null ) {
            return new byte[0];
        }
        return bytes( s, ASCII_ENCODING );
    }


    public ByteBuffer asciibuffer( String s ) {
        return ByteBuffer.wrap( ascii( s ) );
    }


    /**
     * @param s
     * @param encoding
     * @return
     */
    public static byte[] bytes( String s, String encoding ) {
        try {
            return s.getBytes( encoding );
        }
        catch ( UnsupportedEncodingException e ) {
            // logger.log(Level.SEVERE, "UnsupportedEncodingException ", e);
            throw new RuntimeException( e );
        }
    }


    public static byte[] bytes( ByteBuffer bb ) {
        byte[] b = new byte[bb.remaining()];
        bb.duplicate().get( b );
        return b;
    }


    public static ByteBuffer bytebuffer( String s, String encoding ) {
        return ByteBuffer.wrap( bytes( s, encoding ) );
    }


    /**
     * @param b
     * @return
     */
    public static byte[] bytes( Boolean b ) {
        byte[] bytes = new byte[1];
        bytes[0] = b ? ( byte ) 1 : 0;
        return bytes;
    }


    public static ByteBuffer bytebuffer( Boolean b ) {
        return ByteBuffer.wrap( bytes( b ) );
    }


    /**
     * @param val
     * @return
     */
    public static byte[] bytes( Long val ) {
        ByteBuffer buf = ByteBuffer.allocate( 8 );
        buf.order( ByteOrder.BIG_ENDIAN );
        buf.putLong( val );
        return buf.array();
    }


    public static ByteBuffer bytebuffer( Long val ) {
        ByteBuffer buf = ByteBuffer.allocate( 8 );
        buf.order( ByteOrder.BIG_ENDIAN );
        buf.putLong( val );
        return ( ByteBuffer ) buf.rewind();
    }


    /**
     * @param obj
     * @return
     */
    public static byte[] bytes( Object obj ) {
        if ( obj == null ) {
            return new byte[0];
        }
        else if ( obj instanceof byte[] ) {
            return ( byte[] ) obj;
        }
        else if ( obj instanceof Long ) {
            return bytes( ( Long ) obj );
        }
        else if ( obj instanceof String ) {
            return bytes( ( String ) obj );
        }
        else if ( obj instanceof UUID ) {
            return bytes( ( UUID ) obj );
        }
        else if ( obj instanceof Boolean ) {
            return bytes( ( Boolean ) obj );
        }
        else if ( obj instanceof Date ) {
            return bytes( ( ( Date ) obj ).getTime() );
        }
        else {
            return bytes( obj.toString() );
        }
    }


    public static ByteBuffer bytebuffer( byte[] bytes ) {
        return ByteBuffer.wrap( bytes );
    }


    public static ByteBuffer bytebuffer( ByteBuffer bytes ) {
        return bytes.duplicate();
    }


    public static ByteBuffer bytebuffer( Object obj ) {
        if ( obj instanceof ByteBuffer ) {
            return ( ( ByteBuffer ) obj ).duplicate();
        }
        return ByteBuffer.wrap( bytes( obj ) );
    }


    public static List<ByteBuffer> bytebuffers( List<?> l ) {
        List<ByteBuffer> results = new ArrayList<ByteBuffer>( l.size() );
        for ( Object o : l ) {
            results.add( bytebuffer( o ) );
        }
        return results;
    }


    /**
     * @param bytes
     * @return
     */
    public static boolean getBoolean( byte[] bytes ) {
        return bytes[0] != 0;
    }


    public static boolean getBoolean( ByteBuffer bytes ) {
        return bytes.slice().get() != 0;
    }


    /**
     * @param bytes
     * @param offset
     * @return
     */
    public static boolean getBoolean( byte[] bytes, int offset ) {
        return bytes[offset] != 0;
    }


    public static boolean getBoolean( Object obj ) {
        if ( obj instanceof Boolean ) {
            return ( Boolean ) obj;
        }
        else if ( obj instanceof String ) {
            return Boolean.parseBoolean( ( String ) obj );
        }
        else if ( obj instanceof Number ) {
            return ( ( Number ) obj ).longValue() > 0;
        }

        return false;
    }


    /**
     * @param obj
     * @return
     */
    public static String string( Object obj ) {
        if ( obj instanceof String ) {
            return ( String ) obj;
        }
        else if ( obj instanceof byte[] ) {
            return string( ( byte[] ) obj );
        }
        else if ( obj instanceof ByteBuffer ) {
            return string( ( ByteBuffer ) obj );
        }
        else if ( obj != null ) {
            return obj.toString();
        }
        return null;
    }


    /**
     * @param bytes
     * @return
     */
    public static String string( byte[] bytes ) {
        if ( bytes == null ) {
            return null;
        }
        return string( bytes, 0, bytes.length, UTF8_ENCODING );
    }


    public static String string( ByteBuffer bytes ) {
        if ( bytes == null ) {
            return null;
        }
        return string( bytes.array(), bytes.arrayOffset() + bytes.position(), bytes.remaining(), UTF8_ENCODING );
    }


    /**
     * @param bytes
     * @param offset
     * @param length
     * @return
     */
    public static String string( byte[] bytes, int offset, int length ) {
        return string( bytes, offset, length, UTF8_ENCODING );
    }


    /**
     * @param bytes
     * @param offset
     * @param length
     * @param encoding
     * @return
     */
    public static String string( byte[] bytes, int offset, int length, String encoding ) {

        if ( length <= 0 ) {
            return "";
        }

        if ( bytes == null ) {
            return "";
        }

        try {
            return new String( bytes, offset, length, encoding );
        }
        catch ( UnsupportedEncodingException e ) {
            // logger.log(Level.SEVERE, "UnsupportedEncodingException ", e);
            throw new RuntimeException( e );
        }
    }


    public static <T> List<String> strings( Collection<T> items ) {
        List<String> strings = new ArrayList<String>();
        for ( T item : items ) {
            strings.add( string( item ) );
        }
        return strings;
    }


    /**
     * @param bytes
     * @param offset
     * @return
     */
    public static String stringFromLong( byte[] bytes, int offset ) {
        if ( bytes.length == 0 ) {
            return "";
        }
        if ( ( bytes.length - offset ) < 8 ) {
            throw new IllegalArgumentException( "A long is at least 8 bytes" );
        }
        return String.valueOf( ByteBuffer.wrap( bytes, offset, 8 ).getLong() );
    }


    /**
     * @param bytes
     * @return
     */
    public static long getLong( byte[] bytes ) {
        return ByteBuffer.wrap( bytes, 0, 8 ).getLong();
    }


    public static long getLong( ByteBuffer bytes ) {
        return bytes.slice().getLong();
    }


    public static long getLong( Object obj ) {
        if ( obj instanceof Long ) {
            return ( Long ) obj;
        }
        if ( obj instanceof Number ) {
            return ( ( Number ) obj ).longValue();
        }
        if ( obj instanceof String ) {
            return NumberUtils.toLong( ( String ) obj );
        }
        if ( obj instanceof Date ) {
            return ( ( Date ) obj ).getTime();
        }
        if ( obj instanceof byte[] ) {
            return getLong( ( byte[] ) obj );
        }
        if ( obj instanceof ByteBuffer ) {
            return getLong( ( ByteBuffer ) obj );
        }
        return 0;
    }


    /**
     * @param bytes
     * @return
     */
    public static int getInt( byte[] bytes ) {
        return ByteBuffer.wrap( bytes, 0, 4 ).getInt();
    }


    public static int getInt( ByteBuffer bytes ) {
        return bytes.slice().getInt();
    }


    public static int getInt( Object obj ) {
        if ( obj instanceof Integer ) {
            return ( Integer ) obj;
        }
        if ( obj instanceof Number ) {
            return ( ( Number ) obj ).intValue();
        }
        if ( obj instanceof String ) {
            return NumberUtils.toInt( ( String ) obj );
        }
        if ( obj instanceof Date ) {
            return ( int ) ( ( Date ) obj ).getTime();
        }
        if ( obj instanceof byte[] ) {
            return getInt( ( byte[] ) obj );
        }
        if ( obj instanceof ByteBuffer ) {
            return getInt( ( ByteBuffer ) obj );
        }
        return 0;
    }


    /**
     * @param bytes
     * @return
     */
    public static float getFloat( byte[] bytes ) {
        return ByteBuffer.wrap( bytes, 0, 4 ).getFloat();
    }


    public static float getFloat( ByteBuffer bytes ) {
        return bytes.slice().getFloat();
    }


    public static float getFloat( Object obj ) {
        if ( obj instanceof Float ) {
            return ( Float ) obj;
        }
        if ( obj instanceof Number ) {
            return ( ( Number ) obj ).floatValue();
        }
        if ( obj instanceof String ) {
            return NumberUtils.toFloat( ( String ) obj );
        }
        if ( obj instanceof Date ) {
            return ( ( Date ) obj ).getTime();
        }
        if ( obj instanceof byte[] ) {
            return getFloat( ( byte[] ) obj );
        }
        if ( obj instanceof ByteBuffer ) {
            return getFloat( ( ByteBuffer ) obj );
        }
        return 0;
    }


    public static double getDouble( byte[] bytes ) {
        return ByteBuffer.wrap( bytes, 0, 8 ).getDouble();
    }


    public static double getDouble( ByteBuffer bytes ) {
        return bytes.slice().getDouble();
    }


    public static double getDouble( Object obj ) {
        if ( obj instanceof Double ) {
            return ( Double ) obj;
        }
        if ( obj instanceof Number ) {
            return ( ( Number ) obj ).doubleValue();
        }
        if ( obj instanceof String ) {
            return NumberUtils.toDouble( ( String ) obj );
        }
        if ( obj instanceof Date ) {
            return ( ( Date ) obj ).getTime();
        }
        if ( obj instanceof byte[] ) {
            return getDouble( ( byte[] ) obj );
        }
        if ( obj instanceof ByteBuffer ) {
            return getDouble( ( ByteBuffer ) obj );
        }
        return 0;
    }


    /**
     * @param type
     * @param bytes
     * @return
     */
    public static Object object( Class<?> type, byte[] bytes ) {

        try {
            if ( Long.class.isAssignableFrom( type ) ) {
                return getLong( bytes );
            }
            else if ( UUID.class.isAssignableFrom( type ) ) {
                return uuid( bytes );
            }
            else if ( String.class.isAssignableFrom( type ) ) {
                return string( bytes );
            }
            else if ( Boolean.class.isAssignableFrom( type ) ) {
                return getBoolean( bytes );
            }
            else if ( Integer.class.isAssignableFrom( type ) ) {
                return getInt( bytes );
            }
            else if ( Double.class.isAssignableFrom( type ) ) {
                return getDouble( bytes );
            }
            else if ( Float.class.isAssignableFrom( type ) ) {
                return getFloat( bytes );
            }
            else if ( byte[].class.isAssignableFrom( type ) ) {
                return bytes;
            }
        }
        catch ( Exception e ) {
            logger.error( "Unable to get object from bytes for type " + type.getName(), e );
        }
        return null;
    }


    public static Object object( Class<?> type, ByteBuffer bytes ) {

        try {
            if ( Long.class.isAssignableFrom( type ) ) {
                return bytes.slice().getLong();
            }
            else if ( UUID.class.isAssignableFrom( type ) ) {
                return uuid( bytes );
            }
            else if ( String.class.isAssignableFrom( type ) ) {
                return string( bytes );
            }
            else if ( Boolean.class.isAssignableFrom( type ) ) {
                return bytes.slice().get() != 0;
            }
            else if ( Integer.class.isAssignableFrom( type ) ) {
                return bytes.slice().getInt();
            }
            else if ( Double.class.isAssignableFrom( type ) ) {
                return bytes.slice().getDouble();
            }
            else if ( Float.class.isAssignableFrom( type ) ) {
                return bytes.slice().getFloat();
            }
            else if ( ByteBuffer.class.isAssignableFrom( type ) ) {
                return bytes.duplicate();
            }
            else if ( byte[].class.isAssignableFrom( type ) ) {
                byte[] b = new byte[bytes.remaining()];
                bytes.slice().get( b );
                return b;
            }
        }
        catch ( Exception e ) {
            logger.error( "Unable to get object from bytes for type " + type.getName(), e );
        }
        return null;
    }


    /**
     * @param bb
     * @param bytes
     * @param len
     * @return
     */
    public static ByteBuffer appendToByteBuffer( ByteBuffer bb, byte[] bytes, int len ) {
        if ( len > bytes.length ) {
            int pos = bb.position();
            bb.put( bytes );
            bb.position( pos + len );
        }
        else {
            bb.put( bytes, 0, len );
        }
        return bb;
    }


    public static Object coerce( Class<?> type, Object obj ) {

        if ( obj == null ) {
            return null;
        }

        if ( type == null ) {
            return obj;
        }

        try {
            if ( Long.class.isAssignableFrom( type ) ) {
                return getLong( obj );
            }
            else if ( UUID.class.isAssignableFrom( type ) ) {
                return uuid( obj );
            }
            else if ( String.class.isAssignableFrom( type ) ) {
                return string( obj );
            }
            else if ( Boolean.class.isAssignableFrom( type ) ) {
                return getBoolean( obj );
            }
            else if ( Integer.class.isAssignableFrom( type ) ) {
                return getInt( obj );
            }
            else if ( Double.class.isAssignableFrom( type ) ) {
                return getDouble( obj );
            }
            else if ( Float.class.isAssignableFrom( type ) ) {
                return getFloat( obj );
            }
            else if ( byte[].class.isAssignableFrom( type ) ) {
                return bytes( obj );
            }
            else if ( ByteBuffer.class.isAssignableFrom( type ) ) {
                return bytebuffer( obj );
            }
        }
        catch ( Exception e ) {
            logger.error( "Unable to get object from bytes for type " + type.getName(), e );
        }
        return null;
    }


    public static Map<String, Object> coerceMap( Map<String, Class<?>> types, Map<String, Object> values ) {
        for ( Map.Entry<String, Object> entry : values.entrySet() ) {
            if ( types.containsKey( entry.getKey() ) ) {
                values.put( entry.getKey(), coerce( types.get( entry.getKey() ), entry.getValue() ) );
            }
        }
        return values;
    }
}
