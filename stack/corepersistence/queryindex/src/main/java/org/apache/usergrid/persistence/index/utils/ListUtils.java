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
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class ListUtils extends org.apache.commons.collections.ListUtils {
    private static final Logger LOG = LoggerFactory.getLogger( ListUtils.class );


    public static <A> A first( List<A> list ) {
        if ( list == null ) {
            return null;
        }
        if ( list.size() == 0 ) {
            return null;
        }
        return list.get( 0 );
    }


    public static <A> A last( List<A> list ) {
        if ( list == null ) {
            return null;
        }
        if ( list.size() == 0 ) {
            return null;
        }
        return list.get( list.size() - 1 );
    }


    public static <A> Integer firstInteger( List<A> list ) {
        A a = first( list );
        if ( a == null ) {
            return null;
        }

        if ( a instanceof Integer ) {
            return ( Integer ) a;
        }

        try {
            return NumberUtils.toInt( ( String ) a );
        }
        catch ( Exception e ) {
            LOG.warn( "Could not convert list item {} to int", a, e );
        }
        return null;
    }


    public static <A> Long firstLong( List<A> list ) {
        A a = first( list );
        if ( a == null ) {
            return null;
        }

        if ( a instanceof Long ) {
            return ( Long ) a;
        }

        try {
            return NumberUtils.toLong( ( String ) a );
        }
        catch ( Exception e ) {
            LOG.warn( "Could not convert list item {} to long", a, e );
        }
        return null;
    }


    public static <A> Boolean firstBoolean( List<A> list ) {
        A a = first( list );
        if ( a == null ) {
            return null;
        }

        if ( a instanceof Boolean ) {
            return ( Boolean ) a;
        }

        try {
            return Boolean.parseBoolean( ( String ) a );
        }
        catch ( Exception e ) {
            LOG.warn( "Could not convert list item {} to boolean", a, e );
        }
        return null;
    }


    public static <A> UUID firstUuid( List<A> list ) {
        A i = first( list );
        if ( i == null ) {
            return null;
        }

        if ( i instanceof UUID ) {
            return ( UUID ) i;
        }

        try {
            return UUIDUtils.tryGetUUID( ( String ) i );
        }
        catch ( Exception e ) {
            LOG.warn( "Could not convert list item {} to UUID", i, e );
        }
        return null;
    }


    public static boolean isEmpty( List<?> list ) {
        return ( list == null ) || ( list.size() == 0 );
    }


    public static <T> List<T> dequeueCopy( List<T> list ) {
        if ( !isEmpty( list ) ) {
            list = list.subList( 1, list.size() );
        }
        return list;
    }


    public static <T> List<T> initCopy( List<T> list ) {
        if ( !isEmpty( list ) ) {
            list = new ArrayList<T>( list );
        }
        else {
            list = new ArrayList<T>();
        }
        return list;
    }


    public static <T> T dequeue( List<T> list ) {
        if ( !isEmpty( list ) ) {
            return list.remove( 0 );
        }
        return null;
    }


    public static <T> List<T> queue( List<T> list, T item ) {
        if ( list == null ) {
            list = new ArrayList<T>();
        }
        list.add( item );
        return list;
    }


    public static <T> List<T> requeue( List<T> list, T item ) {
        if ( list == null ) {
            list = new ArrayList<T>();
        }
        list.add( 0, item );
        return list;
    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static List<?> flatten( Collection<?> l ) {
        boolean hasCollection = false;
        for ( Object o : l ) {
            if ( o instanceof Collection ) {
                hasCollection = true;
                break;
            }
        }
        if ( !hasCollection && ( l instanceof List ) ) {
            return ( List<?> ) l;
        }
        List newList = new ArrayList();
        for ( Object o : l ) {
            if ( o instanceof List ) {
                newList.addAll( flatten( ( List ) o ) );
            }
            else {
                newList.add( o );
            }
        }
        return newList;
    }


    public static boolean anyNull( List<?> l ) {
        for ( Object o : l ) {
            if ( o == null ) {
                return true;
            }
        }
        return false;
    }


    public static boolean anyNull( Object... objects ) {
        for ( Object o : objects ) {
            if ( o == null ) {
                return true;
            }
        }
        return false;
    }
}
