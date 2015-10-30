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
package org.apache.usergrid.rest.test.resource;


import org.apache.usergrid.rest.test.resource.endpoints.NamedResource;
import org.apache.usergrid.rest.test.resource.endpoints.UrlResource;
import org.apache.usergrid.rest.test.resource.model.ApiResponse;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.rest.test.resource.state.ClientContext;

import javax.ws.rs.core.MediaType;


/**
 *Adds support for changing the management properties in the rest testing framework.
 */
public class TestPropertiesResource extends NamedResource {
    public TestPropertiesResource( final ClientContext context, final UrlResource parent ) {
        super( "testproperties", context, parent );
    }

    public ApiResponse post(Entity testProperties){

        return getTarget( true ).request()
            .accept( MediaType.APPLICATION_JSON )
            .post( javax.ws.rs.client.Entity.json( testProperties), ApiResponse.class);
    }

    public ApiResponse get(){
        return getTarget(true).request()
            .accept( MediaType.APPLICATION_JSON )
            .get( ApiResponse.class );
    }
}
