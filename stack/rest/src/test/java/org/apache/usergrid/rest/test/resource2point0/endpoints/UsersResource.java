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
import org.apache.usergrid.rest.test.resource2point0.model.User;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;

import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * Classy class class.
 */
public class UsersResource extends AbstractCollectionResource<User,UserResource> {

    public UsersResource( ClientContext context, UrlResource parent) {
        super("users", context, parent);
    }

    @Override
    protected User instantiateT(ApiResponse response) {
        return new User(response);
    }

    @Override
    protected UserResource instantiateSubResource(String name, ClientContext context, UrlResource parent) {
        return new UserResource(name,context,parent);
    }
}
