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
package org.apache.usergrid.rest.system;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.usergrid.corepersistence.asyncevents.AsyncEventService;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.security.annotations.RequireSystemAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * retrieves queue stats
 */
@Component
@Scope( "singleton" )
@Produces( {
    MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
    "application/ecmascript", "text/jscript"
} )
public class QueueSystemResource extends AbstractContextResource {
    private static final Logger logger = LoggerFactory.getLogger(QueueSystemResource.class);

    public QueueSystemResource(){logger.info("queue resource initialized");}

    /**
     * Return queue depth of this Usergrid instance in JSON format.
     *
     * By Default this end-point will ignore errors but if you call it with ignore_status=false
     * then it will return HTTP 500 if either the Entity store or the Index for the management
     * application are in a bad state.
     *
     */
    @GET
    @RequireSystemAccess
    @Path("size")
    public ApiResponse getQueueDepth(
        @QueryParam("callback") @DefaultValue("callback") String callback ) {

        ApiResponse response = createApiResponse();
        response.setAction( "get queue depth" );

        AsyncEventService eventService = injector.getInstance(AsyncEventService.class);
        ObjectNode node = JsonNodeFactory.instance.objectNode();

        node.put("queueDepth", eventService.getQueueDepth());

        response.setProperty( "data", node );

        return response;
    }

}
