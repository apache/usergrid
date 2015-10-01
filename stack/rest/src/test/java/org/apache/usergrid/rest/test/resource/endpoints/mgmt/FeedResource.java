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


import javax.ws.rs.core.MediaType;

import org.apache.usergrid.rest.test.resource.endpoints.NamedResource;
import org.apache.usergrid.rest.test.resource.endpoints.UrlResource;
import org.apache.usergrid.rest.test.resource.model.ApiResponse;
import org.apache.usergrid.rest.test.resource.model.Collection;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.rest.test.resource.state.ClientContext;


/**
 * Contains the REST methods to interacting with the ManagementEndpoints
 * and the user feeds
 */
public class FeedResource extends NamedResource {
    public FeedResource(final ClientContext context, final UrlResource parent) {
        super ( "feed",context, parent);
    }

    public ApiResponse get() {
        return getTarget( true ).request()
            .accept( MediaType.APPLICATION_JSON )
            .get( ApiResponse.class);


    }
}
