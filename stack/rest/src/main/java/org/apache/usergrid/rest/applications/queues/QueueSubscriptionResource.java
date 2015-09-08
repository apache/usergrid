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
package org.apache.usergrid.rest.applications.queues;


import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.mq.QueueManager;
import org.apache.usergrid.mq.QueueSet;
import org.apache.usergrid.rest.AbstractContextResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;


@Component
@Scope("prototype")
@Produces({
        MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
        "application/ecmascript", "text/jscript"
})
public class QueueSubscriptionResource extends AbstractContextResource {

    static final Logger logger = LoggerFactory.getLogger( QueueSubscriptionResource.class );

    QueueManager mq;
    String queuePath = "";
    String subscriptionPath = "";


    public QueueSubscriptionResource() {
    }


    public QueueSubscriptionResource init( QueueManager mq, String queuePath ) {
        this.mq = mq;
        this.queuePath = queuePath;
        return this;
    }


    public QueueSubscriptionResource init( QueueManager mq, String queuePath, String subscriptionPath )
            throws Exception {
        this.mq = mq;
        this.queuePath = queuePath;
        this.subscriptionPath = subscriptionPath;
        return this;
    }


    @Path("{subPath}")
    public QueueSubscriptionResource getSubPath( @Context UriInfo ui, @PathParam("subPath") String subPath )
            throws Exception {

        logger.info( "QueueSubscriptionResource.getSubPath" );

        return getSubResource( QueueSubscriptionResource.class )
                .init( mq, queuePath, subscriptionPath + "/" + subPath );
    }


    @GET
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public QueueSet executeGet( @Context UriInfo ui, @QueryParam("start") String firstSubscriptionQueuePath,
                                       @QueryParam("limit") @DefaultValue("10") int limit,
                                       @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        logger.info( "QueueSubscriptionResource.executeGet: " + queuePath );

        QueueSet results = mq.getSubscriptions( queuePath, firstSubscriptionQueuePath, limit );

        return results;
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public QueueSet executePost( @Context UriInfo ui, Map<String, Object> body,
                                        @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        logger.info( "QueueSubscriptionResource.executePost: " + queuePath );

        return executePut( ui, body, callback );
    }


    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public QueueSet executePut( @Context UriInfo ui, Map<String, Object> body,
                                       @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        logger.info( "QueueSubscriptionResource.executePut: " + queuePath );

        Map<String, Object> json = body;
        if ( StringUtils.isNotBlank( subscriptionPath ) ) {
            return mq.subscribeToQueue( subscriptionPath, queuePath );
        }
        else if ( ( json != null ) && ( json.containsKey( "subscriber" ) ) ) {
            String supscription = ( String ) json.get( "supscription" );
            return mq.subscribeToQueue( supscription, queuePath );
        }
        else if ( ( json != null ) && ( json.containsKey( "subscribers" ) ) ) {
            @SuppressWarnings("unchecked") List<String> supscriptions = ( List<String> ) json.get( "supscriptions" );
            return  mq.unsubscribeFromQueues( queuePath, supscriptions );
        }

        return null;
    }


    @DELETE
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public QueueSet executeDelete( @Context UriInfo ui,
                                          @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        logger.info( "QueueSubscriptionResource.executeDelete: " + queuePath );

        if ( StringUtils.isNotBlank( subscriptionPath ) ) {
            return  mq.unsubscribeFromQueue( subscriptionPath, queuePath );
        }

        return null;
    }
}
