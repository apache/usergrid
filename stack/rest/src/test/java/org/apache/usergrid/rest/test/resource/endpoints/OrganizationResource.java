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


import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.apache.usergrid.rest.test.resource.model.ApiResponse;
import org.apache.usergrid.rest.test.resource.model.Organization;
import org.apache.usergrid.rest.test.resource.model.Token;
import org.apache.usergrid.rest.test.resource.state.ClientContext;


/**
 * Holds the information required for building and chaining organization objects to applications.
 * Should also contain the GET,PUT,POST,DELETE methods of functioning in here.
 */
public class OrganizationResource extends NamedResource {


    public OrganizationResource( final String name, final ClientContext context, final UrlResource parent ) {
        super( name, context, parent );
    }

    public ApplicationsResource app(final String name){
        return new ApplicationsResource( name, context ,this );
    }

    public Organization get(){
        throw new UnsupportedOperationException("service doesn't exist");
    }
}
