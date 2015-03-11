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


import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.usergrid.persistence.index.utils.ConversionUtils.string;


public class StringUtils extends org.apache.commons.lang.StringUtils {

    private static final Logger LOG = LoggerFactory.getLogger( StringUtils.class );



    public static String stringOrSubstringBeforeFirst( String str, char c ) {
        if ( str == null ) {
            return null;
        }
        int i = str.indexOf( c );
        if ( i != -1 ) {
            return str.substring( 0, i );
        }
        return str;
    }


    public static String toString( Object obj ) {
        return string( obj );
    }


    /**
     * Remove dashes from our uuid
     * @param uuid
     * @return
     */
    public static String sanitizeUUID(final UUID uuid){
        return uuid.toString().replace( "-", "" );
    }
}
