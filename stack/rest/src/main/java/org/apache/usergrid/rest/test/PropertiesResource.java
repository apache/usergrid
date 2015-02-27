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
package org.apache.usergrid.rest.test;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.apache.usergrid.rest.AbstractContextResource;


/**
 * Set properties at runtime, for testing purposes only and only works with usergrid.test=true.
 */
@Component
@Scope("prototype")
@Path("/testproperties")
@Produces({ MediaType.APPLICATION_JSON })
public class PropertiesResource extends AbstractContextResource {
    static final Logger logger = LoggerFactory.getLogger( PropertiesResource.class );


    public PropertiesResource() {}


    @POST
    public Response setProperties( String body ) throws IOException {

        Properties props = management.getProperties();

        // only works in test mode
        String testProp = ( String ) props.get( "usergrid.test" );
        if ( testProp == null || !Boolean.parseBoolean( testProp ) ) {
            throw new UnsupportedOperationException();
        }

        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> map = mapper.readValue( body, new TypeReference<Map<String, String>>() {} );
        for ( String key : map.keySet() ) {
            management.getProperties().setProperty( key, map.get( key ) );
        }

        return Response.created( null ).build();
    }


    @GET
    public Response getProperties() throws Exception {

        Properties props = management.getProperties();

        // only works in test mode
        String testProp = ( String ) props.get( "usergrid.test" );
        if ( testProp == null || !Boolean.parseBoolean( testProp ) ) {
            throw new UnsupportedOperationException();
        }

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString( props );
        return Response.ok( json ).build();
    }
}
