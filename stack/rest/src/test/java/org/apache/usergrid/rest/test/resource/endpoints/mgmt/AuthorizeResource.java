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
package org.apache.usergrid.rest.test.resource.endpoints.mgmt;

import org.apache.usergrid.rest.test.resource.endpoints.NamedResource;
import org.apache.usergrid.rest.test.resource.endpoints.UrlResource;
import org.apache.usergrid.rest.test.resource.state.ClientContext;

/**
 * OAuth authorization resource
 */
public class AuthorizeResource extends NamedResource {
    public AuthorizeResource(final ClientContext context, final UrlResource parent) {
        super("authorize", context, parent);
    }

    /**
     * Obtains an OAuth authorization
     *
     * @param requestEntity
     * @return
     */
    public Object post(Object requestEntity) {
        return getTarget().request().post( javax.ws.rs.client.Entity.json(requestEntity), Object.class);

    }

    /**
     * Obtains an OAuth authorization
     *
     * @param type
     * @param requestEntity
     * @return
     */
    public <T> T post(Class<T> type, Object requestEntity) {
        return getTarget().request().post( javax.ws.rs.client.Entity.json( requestEntity ), type );
    }

}
