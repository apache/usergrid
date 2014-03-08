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
package org.apache.usergrid.persistence;


import java.util.Map;
import java.util.TreeMap;


public class EntityUtils {

    public static Map<String, Object> getSchemaProperties( String entityType, Map<String, Object> properties ) {

        Map<String, Object> sys_props = new TreeMap<String, Object>( String.CASE_INSENSITIVE_ORDER );

        for ( String propName : properties.keySet() ) {
            if ( Schema.getDefaultSchema().hasProperty( entityType, propName ) ) {
                Object propValue = properties.get( propName );
                sys_props.put( propName, propValue );
            }
        }

        return sys_props;
    }


    public static Map<String, Object> getDynamicProperties( String entityType, Map<String, Object> properties ) {

        Map<String, Object> sys_props = new TreeMap<String, Object>( String.CASE_INSENSITIVE_ORDER );

        for ( String propName : properties.keySet() ) {
            if ( !Schema.getDefaultSchema().hasProperty( entityType, propName ) ) {
                Object propValue = properties.get( propName );
                sys_props.put( propName, propValue );
            }
        }

        return sys_props;
    }
}
