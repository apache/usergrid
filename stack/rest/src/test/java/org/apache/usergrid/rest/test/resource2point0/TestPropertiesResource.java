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
package org.apache.usergrid.rest.test.resource2point0;


import javax.ws.rs.core.MediaType;

import org.apache.usergrid.rest.test.resource2point0.endpoints.NamedResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.UrlResource;
import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;


/**
 *Adds support for changing the management properties in the rest testing framework.
 */
public class TestPropertiesResource extends NamedResource {
    public TestPropertiesResource( final ClientContext context, final UrlResource parent ) {
        super( "testproperties", context, parent );
    }

    public ApiResponse post(Entity testProperties){

        return getResource(true).type( MediaType.APPLICATION_JSON_TYPE )
                            .accept( MediaType.APPLICATION_JSON ).post( ApiResponse.class, testProperties );
    }

    public ApiResponse get(){
        return getResource(true).type( MediaType.APPLICATION_JSON_TYPE )
                       .accept( MediaType.APPLICATION_JSON ).get(ApiResponse.class );
    }
}
