/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */

package org.apache.usergrid.rest.test.resource2point0.endpoints.mgmt;

import javax.ws.rs.core.MediaType;

import org.apache.usergrid.rest.test.resource2point0.endpoints.NamedResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.UrlResource;
import org.apache.usergrid.rest.test.resource2point0.model.Application;
import org.apache.usergrid.rest.test.resource2point0.model.*;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;


/**
 * Classy class class.
 */
public class ApplicationResource extends NamedResource {
    public ApplicationResource(ClientContext context, UrlResource parent) {
        super("applications", context, parent);
    }

    public ApplicationResource( final String name, final ClientContext context, final UrlResource parent ) {
        super( name, context, parent );
    }

    public ApplicationResource addToPath( String pathPart ) {
        return new ApplicationResource( pathPart, context, this );
    }


    public void post(Application application) {
        getResource(true).type(MediaType.APPLICATION_JSON_TYPE)
            .accept(MediaType.APPLICATION_JSON).post(application);
    }

    public Entity post(Entity payload){
        ApiResponse response = getResource(true).type( MediaType.APPLICATION_JSON_TYPE ).accept(MediaType.APPLICATION_JSON)
            .post(ApiResponse.class, payload);
        return new Entity(response);
    }


    public Entity get() {
        ApiResponse response = getResource(true).type(MediaType.APPLICATION_JSON_TYPE ).accept(MediaType.APPLICATION_JSON)
            .get(ApiResponse.class);

        return new Entity(response);
    }
}
