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
package org.apache.usergrid.count.common;


import java.io.IOException;

//import org.codehaus.jackson.annotate.JsonAutoDetect;
//import org.codehaus.jackson.annotate.JsonMethod;
//import org.codehaus.jackson.map.ObjectMapper;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;



/** @author zznate */
public class CountSerDeUtils {

    public static String serialize( Count count ) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString( count );
        }
        catch ( Exception ex ) {
            throw new CountTransportSerDeException( "Problem in serialize() call", ex );
        }
    }


    public static Count deserialize( String json ) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility( PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY );

        try {
            return mapper.readValue( json, Count.class );
        }
        catch ( IOException e ) {
            throw new CountTransportSerDeException( "Problem in deserialize() call", e );
        }
    }
}
