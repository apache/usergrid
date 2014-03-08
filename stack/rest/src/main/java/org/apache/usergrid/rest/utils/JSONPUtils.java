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
package org.apache.usergrid.rest.utils;


import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MediaType;

import org.apache.usergrid.security.oauth.AccessInfo;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.usergrid.utils.JsonUtils.mapToJsonString;


public class JSONPUtils {

    static Map<String, Set<String>> javascriptTypes = new HashMap<String, Set<String>>();


    static {
        // application/javascript, application/x-javascript, text/ecmascript,
        // application/ecmascript, text/jscript
        javascriptTypes.put( "application",
                new HashSet<String>( Arrays.asList( "x-javascript", "ecmascript", "javascript" ) ) );
        javascriptTypes.put( "text", new HashSet<String>( Arrays.asList( "ecmascript", "jscript" ) ) );
    }


    public static boolean isJavascript( MediaType m ) {
        if ( m == null ) {
            return false;
        }

        Set<String> subtypes = javascriptTypes.get( m.getType() );
        if ( subtypes == null ) {
            return false;
        }

        return subtypes.contains( m.getSubtype() );
    }


    public static boolean isJavascript( List<MediaType> l ) {
        for ( MediaType m : l ) {
            if ( isJavascript( m ) ) {
                return true;
            }
        }
        return false;
    }


    public static String wrapJSONPResponse( String callback, String jsonResponse ) {
        if ( isNotBlank( callback ) ) {
            return callback + "(" + jsonResponse + ")";
        }
        else {
            return jsonResponse;
        }
    }


    public static String wrapJSONPResponse( MediaType m, String callback, String jsonResponse ) {
        if ( isJavascript( m ) && isNotBlank( callback ) ) {
            String jsResponse = callback + "(" + jsonResponse + ")";
            return jsResponse;
        }
        else {
            return jsonResponse;
        }
    }


    public static MediaType jsonMediaType( String callback ) {
        return isNotBlank( callback ) ? new MediaType( "application", "javascript" ) : APPLICATION_JSON_TYPE;
    }


    public static String wrapWithCallback( AccessInfo accessInfo, String callback ) {
        return wrapJSONPResponse( callback, mapToJsonString( accessInfo ) );
    }
}
