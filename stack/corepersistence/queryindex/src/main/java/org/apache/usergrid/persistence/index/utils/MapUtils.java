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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import static org.apache.usergrid.persistence.index.utils.ClassUtils.cast;


public class MapUtils extends org.apache.commons.collections.MapUtils {

    public static <A, B> void addMapSet( Map<A, Set<B>> map, A a, B b ) {
        addMapSet( map, false, a, b );
    }


    @SuppressWarnings("unchecked")
    public static <A, B> void addMapSet( Map<A, Set<B>> map, boolean ignoreCase, A a, B b ) {

        Set<B> setB = map.get( a );
        if ( setB == null ) {
            if ( ignoreCase && ( b instanceof String ) ) {
                setB = ( Set<B> ) new TreeSet<String>( String.CASE_INSENSITIVE_ORDER );
            }
            else {
                setB = new LinkedHashSet<B>();
            }
            map.put( a, setB );
        }
        setB.add( b );
    }


    public static <A, B, C> void addMapMapSet( Map<A, Map<B, Set<C>>> map, A a, B b, C c ) {
        addMapMapSet( map, false, a, b, c );
    }


    @SuppressWarnings("unchecked")
    public static <A, B, C> void addMapMapSet( Map<A, Map<B, Set<C>>> map, boolean ignoreCase, A a, B b, C c ) {

        Map<B, Set<C>> mapB = map.get( a );
        if ( mapB == null ) {
            if ( ignoreCase && ( b instanceof String ) ) {
                mapB = ( Map<B, Set<C>> ) new TreeMap<String, Set<C>>( String.CASE_INSENSITIVE_ORDER );
            }
            else {
                mapB = new LinkedHashMap<B, Set<C>>();
            }
            map.put( a, mapB );
        }
        addMapSet( mapB, ignoreCase, b, c );
    }


    @SuppressWarnings("unchecked")
    public static <A, B, C, D> void addMapMapMapSet( Map<A, Map<B, Map<C, Set<D>>>> map, boolean ignoreCase, A a, B b,
                                                     C c, D d ) {
        Map<B, Map<C, Set<D>>> mapB = map.get( a );
        if ( mapB == null ) {
            if ( ignoreCase && ( b instanceof String ) ) {
                mapB = ( Map<B, Map<C, Set<D>>> ) new TreeMap<String, Map<C, Set<D>>>( String.CASE_INSENSITIVE_ORDER );
            }
            else {
                mapB = new LinkedHashMap<B, Map<C, Set<D>>>();
            }
            map.put( a, mapB );
        }
        addMapMapSet( mapB, ignoreCase, b, c, d );
    }


    public static <A, B, C> C getMapMap( Map<A, Map<B, C>> map, A a, B b ) {

        Map<B, C> mapB = map.get( a );
        if ( mapB == null ) {
            return null;
        }
        return mapB.get( b );
    }


    public static <A, B> void addMapList( Map<A, List<B>> map, A a, B b ) {

        List<B> listB = map.get( a );
        if ( listB == null ) {
            listB = new ArrayList<B>();
            map.put( a, listB );
        }
        listB.add( b );
    }


    public static <A, B> void addListToMapList( Map<A, List<B>> map, A a, List<B> b ) {

        List<B> listB = map.get( a );
        if ( listB == null ) {
            listB = new ArrayList<B>();
            map.put( a, listB );
        }
        listB.addAll( b );
    }


    @SuppressWarnings("unchecked")
    public static <K, V> V getValue( Map<K, ?> map, K k ) {
        V v = null;
        try {
            v = ( V ) map.get( k );
        }
        catch ( ClassCastException e ) {
            //LOG.war( "Map value {} was not the expected class", map.get( k ), e );
        }

        return v;
    }


    @SuppressWarnings("unchecked")
    public static <K, V> Map<?, ?> map( Object... objects ) {
        Map<K, V> map = new LinkedHashMap<K, V>();
        int i = 0;
        while ( i < objects.length ) {
            if ( objects[i] instanceof Map.Entry ) {
                Map.Entry<K, V> entry = ( Entry<K, V> ) objects[i];
                map.put( entry.getKey(), entry.getValue() );
                i++;
            }
            else if ( objects[i] instanceof Map ) {
                map.putAll( ( Map<? extends K, ? extends V> ) objects[i] );
                i++;
            }
            else if ( i < ( objects.length - 1 ) ) {
                K k = ( K ) objects[i];
                V v = ( V ) objects[i + 1];
                map.put( k, v );
                i += 2;
            }
            else {
                break;
            }
        }
        return map;
    }


    private static class SimpleMapEntry<K, V> implements Map.Entry<K, V> {

        private final K k;
        private V v;


        public SimpleMapEntry( K k, V v ) {
            this.k = k;
            this.v = v;
        }


        @Override
        public K getKey() {
            return k;
        }


        @Override
        public V getValue() {
            return v;
        }


        @Override
        public V setValue( V v ) {
            V oldV = this.v;
            this.v = v;
            return oldV;
        }
    }


    public static <K, V> Map.Entry<K, V> entry( K k, V v ) {
        return new SimpleMapEntry<K, V>( k, v );
    }


    public static <K, V> K getFirstKey( Map<K, V> map ) {
        if ( map == null ) {
            return null;
        }
        Entry<K, V> e = map.entrySet().iterator().next();
        if ( e != null ) {
            return e.getKey();
        }
        return null;
    }


    public static <V> Map<String, V> filter( Map<String, V> map, String prefix, boolean removePrefix ) {
        Map<String, V> filteredMap = new LinkedHashMap<String, V>();
        for ( Entry<String, V> entry : map.entrySet() ) {
            if ( entry.getKey().startsWith( prefix ) ) {
                if ( removePrefix ) {
                    filteredMap.put( entry.getKey().substring( prefix.length() ), entry.getValue() );
                }
                else {
                    filteredMap.put( entry.getKey(), entry.getValue() );
                }
            }
        }
        return filteredMap;
    }


    public static <V> Map<String, V> filter( Map<String, V> map, String prefix ) {
        return filter( map, prefix, false );
    }


    public static Properties filter( Properties properties, String prefix, boolean removePrefix ) {
        Properties filteredProperties = new Properties();
        for ( Entry<String, String> entry : asMap( properties ).entrySet() ) {
            if ( entry.getKey().startsWith( prefix ) ) {
                if ( removePrefix ) {
                    filteredProperties.put( entry.getKey().substring( prefix.length() ), entry.getValue() );
                }
                else {
                    filteredProperties.put( entry.getKey(), entry.getValue() );
                }
            }
        }
        return filteredProperties;
    }


    public static Properties filter( Properties properties, String prefix ) {
        return filter( properties, prefix, false );
    }


    @SuppressWarnings("unchecked")
    public static Map<String, String> asMap( Properties properties ) {
        return cast( properties );
    }


    public static <S, T> HashMapBuilder<S, T> hashMap( S key, T value ) {
        return new HashMapBuilder<S, T>().map( key, value );
    }


    public static class HashMapBuilder<S, T> extends HashMap<S, T> {
        private static final long serialVersionUID = 1L;


        public HashMapBuilder() {
        }


        public HashMapBuilder<S, T> map( S key, T value ) {
            put( key, value );
            return this;
        }
    }


    @SuppressWarnings("unchecked")
    public static Map<String, List<?>> toMapList( Map<String, ?> m ) {
        Map<String, List<Object>> mapList = new LinkedHashMap<String, List<Object>>();

        for ( Entry<String, ?> e : m.entrySet() ) {
            if ( e.getValue() instanceof List ) {
                addListToMapList( mapList, e.getKey(), ( List<Object> ) e.getValue() );
            }
            else {
                addMapList( mapList, e.getKey(), e.getValue() );
            }
        }

        return cast( mapList );
    }


    public static Map<String, ?> putPath( String path, Object value ) {
        return putPath( null, path, value );
    }


    @SuppressWarnings("unchecked")
    public static Map<String, ?> putPath( Map<String, ?> map, String path, Object value ) {

        if ( map == null ) {
            map = new HashMap<String, Object>();
        }

        int i = path.indexOf( '.' );
        if ( i < 0 ) {
            ( ( Map<String, Object> ) map ).put( path, value );
            return map;
        }
        String segment = path.substring( 0, i ).trim();
        if ( isNotBlank( segment ) ) {
            Object o = map.get( segment );
            if ( ( o != null ) && ( !( o instanceof Map ) ) ) {
                return map;
            }
            Map<String, Object> subMap = ( Map<String, Object> ) o;
            if ( subMap == null ) {
                subMap = new HashMap<String, Object>();
                ( ( Map<String, Object> ) map ).put( segment, subMap );
            }
            String subPath = path.substring( i + 1 );
            if ( isNotBlank( subPath ) ) {
                putPath( subMap, subPath, value );
            }
        }

        return map;
    }


    public static <K, V> Map<K, V> emptyMapWithKeys( Map<K, V> map ) {
        Map<K, V> newMap = new HashMap<K, V>();

        for ( K k : map.keySet() ) {
            newMap.put( k, null );
        }

        return newMap;
    }


    public static boolean hasKeys( Map<?, ?> map, String... keys ) {
        if ( map == null ) {
            return false;
        }
        for ( String key : keys ) {
            if ( !map.containsKey( key ) ) {
                return false;
            }
        }
        return true;
    }


    public static boolean hasKeys( Map<?, ?> map, Set<String> keys ) {
        if ( map == null ) {
            return false;
        }
        return map.keySet().containsAll( keys );
    }
}
