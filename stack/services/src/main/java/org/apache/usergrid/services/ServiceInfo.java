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
package org.apache.usergrid.services;


import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.usergrid.persistence.Schema;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import static org.apache.commons.lang.StringUtils.capitalize;
import static org.apache.commons.lang.StringUtils.removeEnd;
import static org.apache.commons.lang.StringUtils.split;
import static org.apache.usergrid.utils.InflectionUtils.pluralize;
import static org.apache.usergrid.utils.InflectionUtils.singularize;
import static org.apache.usergrid.utils.StringUtils.stringOrSubstringAfterLast;
import static org.apache.usergrid.utils.StringUtils.stringOrSubstringBeforeFirst;
import static org.apache.usergrid.utils.StringUtils.stringOrSubstringBeforeLast;


public class ServiceInfo {

    public static final Charset UTF_8 = Charset.forName( "UTF-8" );

    private final String name;
    private final boolean rootService;
    private final String rootType;
    private final String containerType;
    private final String collectionName;
    private final String itemType;
    private final List<String> patterns;
    private final List<String> collections;

    /** Pre calced since we cache this class */
    private int hashCode;


    public ServiceInfo( String name, boolean rootService, String rootType, String containerType, String collectionName,
                        String itemType, List<String> patterns, List<String> collections ) {
        this.name = name;
        this.rootService = rootService;
        this.rootType = rootType;
        this.containerType = containerType;
        this.collectionName = collectionName;
        this.itemType = itemType;
        this.patterns = patterns;
        this.collections = collections;

        Hasher hasher = Hashing.md5().newHasher();

        for ( String pattern : patterns ) {
            hasher.putString( pattern, UTF_8 );
        }

        hashCode = hasher.hash().asInt();
    }


    public static String normalizeServicePattern( String s ) {
        if ( s == null ) {
            return null;
        }
        s = s.trim().toLowerCase();

        s = removeEnd( s, "/" );
        s = removeEnd( s, "/*" );

        if ( !s.startsWith( "/" ) ) {
            s = "/" + s;
        }

        return s;
    }


    public static List<String> getPatterns( String servicePattern ) {

        String[] collections = split( servicePattern, "/*/" );
        return getPatterns( servicePattern, collections );
    }


    public static List<String> getPatterns( String servicePattern, String[] collections ) {

        if ( collections == null ) {
            collections = split( servicePattern, "/*/" );
        }

        List<String> patterns = new ArrayList<String>();
        patterns.add( servicePattern );
        if ( servicePattern.indexOf( ':' ) >= 0 ) {
            patterns.add( removeTypeSpecifiers( collections ) );
        }

        String s = getFallbackPattern( collections, 0, collections.length - 1 );
        while ( s != null ) {
            patterns.add( s );
            s = getFallbackPattern( s );
        }

        return patterns;
    }


    private static String removeTypeSpecifiers( String[] collections ) {
        String s = "";
        boolean first = true;

        for ( String collection : collections ) {
            if ( !first ) {
                s += "/*";
            }
            first = false;
            s += "/" + stringOrSubstringBeforeFirst( collection, ':' );
        }
        return s;
    }


    private static String getFallbackPattern( String servicePattern ) {
        String[] collections = split( servicePattern, "/*/" );
        return getFallbackPattern( collections, 0, collections.length - 1 );
    }


    private static String getFallbackPattern( String[] collections, int first, int last ) {

        if ( last < first ) {
            return null;
        }

        if ( ( last - first ) == 1 ) {
            if ( !collections[first].startsWith( "entities" ) ) {
                return "/entities:" + singularize( collections[first] ) + "/*/" + collections[first + 1];
            }
            return null;
        }

        if ( ( last - first ) == 0 ) {
            if ( !collections[first].startsWith( "entities" ) ) {
                return "/entities:" + singularize( collections[first] );
            }
            return null;
        }

        int i = last - 1;
        while ( i >= first ) {
            if ( collections[i].indexOf( ':' ) > -1 ) {
                break;
            }
            i--;
        }

        if ( i >= first ) {
            String type = stringOrSubstringAfterLast( collections[i], ':' );
            String fallback = "/" + pluralize( type );
            i++;
            while ( i <= last ) {
                fallback += "/*/" + collections[i];
                i++;
            }
            return fallback;
        }

        String eType = determineType( collections, first, last - 1 );
        if (!eType.equals("entity")) {
            return "/entities:" + eType + "/*/" + collections[last];
        }

        return "/entities/*/" + collections[last];
    }


    public static String determineType( String servicePattern ) {

        String[] collections = split( servicePattern, '/' );
        return determineType( collections, 0, collections.length - 1 );
    }


    private static String determineType( String[] collections, int first, int last ) {

        if ( last < first ) {
            return null;
        }

        if ( first == last ) {
            return singularize( stringOrSubstringAfterLast( collections[0], ':' ) );
        }

        int i = first + 1;
        String containerType = singularize( collections[first] );

        while ( i <= last ) {
            String collectionName = stringOrSubstringBeforeFirst( collections[i], ':' );
            String nextType = Schema.getDefaultSchema().getCollectionType( containerType, collectionName );
            if ( nextType == null ) {
                if ( collections[i].indexOf( ':' ) >= 0 ) {
                    nextType = stringOrSubstringAfterLast( collections[i], ':' );
                }
                else if ( ( i < last ) && ( collections[last].indexOf( ':' ) >= 0 ) ) {
                    nextType = stringOrSubstringAfterLast( collections[last], ':' );
                }
                else {
                    return "entity";
                }
            }
            containerType = nextType;
            i++;
        }
        return containerType;
    }


    /** Hold servicePattern names in a fixed size cache */
    private static LoadingCache<String, String> servicePatternCache =
            CacheBuilder.newBuilder().maximumSize( 5000 ).build( new CacheLoader<String, String>() {
                public String load( String key ) { // no checked exception
                    return _getClassName( key );
                }
            } );


    /** Delegates to _getClassName via a CacheLoader due to the expense of path name calculation */
    public static String getClassName( String servicePattern ) {
        try {
            return servicePatternCache.get( servicePattern );
        }
        catch ( ExecutionException ee ) {
            ee.printStackTrace();
        }
        return _getClassName( servicePattern );
    }


    private static String _getClassName( String servicePattern ) {
        servicePattern = normalizeServicePattern( servicePattern );

        String[] collections = split( servicePattern, "/*/" );

        if ( collections[0].startsWith( "entities" ) ) {
            if ( collections.length == 1 ) {
                return "generic.RootCollectionService";
            }
            if ( collections[0].indexOf( ':' ) < 0 ) {
                return "generic.GenericConnectionsService";
            }
            String container = stringOrSubstringAfterLast( collections[0], ':' );
            String collectionName = stringOrSubstringBeforeFirst( collections[1], ':' );
            if ( Schema.getDefaultSchema().hasCollection( container, collectionName ) ) {
                return "generic.GenericCollectionService";
            }
            return "generic.GenericConnectionsService";
        }

        String packages = "";

        String types = "";

        if ( collections.length == 1 ) {
            packages = stringOrSubstringBeforeLast( stringOrSubstringBeforeFirst( collections[0], ':' ), '.' ) + ".";
        }
        else {
            for ( int i = 0; i < collections.length; i++ ) {
                if ( i == 0 ) {
                    packages = stringOrSubstringBeforeFirst( collections[i], ':' ) + ".";
                }
                else {
                    packages += stringOrSubstringBeforeLast( stringOrSubstringBeforeFirst( collections[i], ':' ), '.' )
                            + ".";
                }
                if ( ( i < ( collections.length - 1 ) ) && ( collections[i].indexOf( ':' ) >= 0 ) ) {
                    types += capitalize(
                            stringOrSubstringAfterLast( stringOrSubstringAfterLast( collections[i], ':' ), '.' ) );
                }
            }
        }

        return packages + types + capitalize(
                stringOrSubstringAfterLast( stringOrSubstringBeforeFirst( collections[collections.length - 1], ':' ),
                        '.' ) ) + "Service";
    }


    private static final Map<String, ServiceInfo> serviceInfoCache = new LinkedHashMap<String, ServiceInfo>();


    public static ServiceInfo getServiceInfo( String servicePattern ) {
        if ( servicePattern == null ) {
            return null;
        }

        servicePattern = normalizeServicePattern( servicePattern );

        ServiceInfo info = serviceInfoCache.get( servicePattern );

        if ( info != null ) {
            return info;
        }

        String[] collections = split( servicePattern, "/*/" );

        if ( collections.length == 0 ) {
            return null;
        }

        String collectionName = stringOrSubstringBeforeFirst( collections[collections.length - 1], ':' );

        if ( collectionName == null ) {
            throw new NullPointerException( "Collection name is null" );
        }

        String ownerType = "entity";

        String rootType = determineType( collections, 0, 0 );

        if ( collections.length == 1 ) {
            ownerType = "application";
        }

        if ( collections.length > 1 ) {
            ownerType = determineType( collections, 0, collections.length - 2 );
        }

        String itemType = determineType( collections, 0, collections.length - 1 );

        List<String> patterns = getPatterns( servicePattern, collections );
        info = new ServiceInfo( servicePattern, collections.length == 1, rootType, ownerType, collectionName, itemType,
                patterns, Arrays.asList( collections ) );

        serviceInfoCache.put( servicePattern, info );

        return info;
    }


    public String getClassName() {
        return getClassName( name );
    }


    public String getName() {
        return name;
    }


    public boolean isRootService() {
        return rootService;
    }


    public String getRootType() {
        return rootType;
    }


    public boolean isGenericRootType() {
        return ( "entity".equals( rootType ) ) || ( "entities".equals( rootType ) );
    }


    public String getContainerType() {
        return containerType;
    }


    public boolean isContainerType() {
        return ( "entity".equals( containerType ) ) || ( "entities".equals( containerType ) );
    }


    public String getCollectionName() {
        return collectionName;
    }


    public String getItemType() {
        return itemType;
    }


    public boolean isGenericItemType() {
        return "entity".equals( itemType );
    }


    public List<String> getPatterns() {
        return patterns;
    }


    public List<String> getCollections() {
        return collections;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if ( obj instanceof ServiceInfo ) {
            return hashCode == ( ( ServiceInfo ) obj ).hashCode;
        }

        return false;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return hashCode;
    }
}
