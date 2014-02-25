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


import java.util.UUID;

import org.apache.commons.codec.binary.Base64;

import static org.apache.usergrid.utils.ConversionUtils.bytes;


public class CodecUtils {

    public static Base64 base64 = new Base64( true );


    public static String base64( byte[] bytes ) {
        return base64.encodeToString( bytes );
    }


    public static String base64( UUID uuid ) {
        return base64.encodeToString( bytes( uuid ) );
    }


    public static String base64( String str ) {
        return Base64.encodeBase64URLSafeString( bytes( str ) );
    }
}
