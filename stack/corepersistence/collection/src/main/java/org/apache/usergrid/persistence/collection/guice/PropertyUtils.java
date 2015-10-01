/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.collection.guice;


import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 * Simple Utility class to get properties
 *
 * @author tnine
 */
public class PropertyUtils {
    /**
     * Load the properties file from the classpath.  Throws IOException if they cannot be loaded
     */
    public static Properties loadFromClassPath( String propsFile ) {
        InputStream in = PropertyUtils.class.getClassLoader().getResourceAsStream( propsFile );

        if ( in == null ) {
            throw new RuntimeException( new IOException(
                    String.format( "Could not find properties file on the classpath at location %s", propsFile ) ) );
        }

        Properties props = new Properties();

        try {
            props.load( in );
        }
        catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        return props;
    }


    /**
     * Filters out key value pairs matching the propvided keys from Properties.
     *
     * @param keys the keys of the property key value pairs to filter out
     * @param properties the properties to filter
     * @return the subset of the properties that have the specified keys
     */
    public static Map<String,Object> filter( String[] keys, Properties properties ) {
        Map<String,Object> filtered = new HashMap<String, Object>();

        for ( String key : keys ) {
            filtered.put( key, properties.getProperty( key ) );
        }

        return filtered;
    }


    /**
     * Filters out key value pairs matching the propvided keys from Properties.
     *
     * @param keys the keys of the property key value pairs to filter out
     * @param properties the properties to filter
     * @return the subset of the properties that have the specified keys
     */
    public static Map<String,Object> filter( String[] keys, Map<String,Object> properties ) {
        Map<String,Object> filtered = new HashMap<String, Object>();

        for ( String key : keys ) {
            if ( properties.get( key ) != null ) {
                filtered.put( key, properties.get( key ) );
            }
        }

        return filtered;
    }


    /**
     * Load each of the defined properties into a system property and return them.  If a system property is not found,
     * it will be ignored
     */
    public static Properties loadSystemProperties( String... properties ) {

        Properties props = new Properties();

        for ( String propName : properties ) {
            String propValue = System.getProperty( propName );

            if ( propValue != null ) {
                props.put( propName, propValue );
            }
        }


        return props;
    }
}
