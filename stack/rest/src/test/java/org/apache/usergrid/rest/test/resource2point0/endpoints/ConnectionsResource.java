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

package org.apache.usergrid.rest.test.resource2point0.endpoints;

import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;

/**
 * /myorg/myapp/collection/id/verborcollection/collection
 */
public class ConnectionsResource extends AbstractCollectionResource<Entity,ConnectionResource> {

    public ConnectionsResource(String verb, String collection,ClientContext context, UrlResource parent) {
        super(verb+"/"+collection, context, parent);
    }
    public ConnectionsResource( String collection,ClientContext context, UrlResource parent) {
        super(collection, context, parent);
    }

    @Override
    protected Entity instantiateT(ApiResponse response) {
        return new Entity(response);
    }

    @Override
    protected ConnectionResource instantiateEntityResource(String identifier, ClientContext context, UrlResource parent) {
        return new ConnectionResource(identifier,context,parent);
    }

    public ConnectionResource connection(String entityId){
        return new ConnectionResource(entityId,context,this);
    }


}
