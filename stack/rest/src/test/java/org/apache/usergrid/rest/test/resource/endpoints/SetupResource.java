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
package org.apache.usergrid.rest.test.resource.endpoints;


import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.rest.test.resource.model.QueryParameters;
import org.apache.usergrid.rest.test.resource.state.ClientContext;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

/**
 * Functions as the endpoint for all resources that hit /system/ * /setup
 */
public class SetupResource extends NamedResource {

    public SetupResource( final ClientContext context, final UrlResource parent ) {
        super("setup",context,parent);
    }

    public Entity put(QueryParameters queryParameters){

        WebTarget resource = getTarget();
        resource = addParametersToResource( resource, queryParameters );

        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder()
            .credentials( "superuser", "superpassword" ).build();

        return resource.register( feature ).request()
            .accept( MediaType.APPLICATION_JSON ).put( javax.ws.rs.client.Entity.json(""), Entity.class );
    }
}
