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
public class QueueSubscriberResource extends AbstractContextResource {

    static final Logger logger = LoggerFactory.getLogger( QueueSubscriberResource.class );

    QueueManager mq;
    String queuePath = "";
    String subscriberPath = "";


    public QueueSubscriberResource() {
    }


    public QueueSubscriberResource init( QueueManager mq, String queuePath ) {
        this.mq = mq;
        this.queuePath = queuePath;
        return this;
    }


    public QueueSubscriberResource init( QueueManager mq, String queuePath, String subscriberPath ) {
        this.mq = mq;
        this.queuePath = queuePath;
        this.subscriberPath = subscriberPath;
        return this;
    }


    @Path("{subPath}")
    public QueueSubscriberResource getSubPath( @Context UriInfo ui, @PathParam("subPath") String subPath )
            throws Exception {

        logger.info( "QueueSubscriberResource.getSubPath" );

        return getSubResource( QueueSubscriberResource.class ).init( mq, queuePath, subscriberPath + "/" + subPath );
    }


    @GET
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public QueueSet executeGet( @Context UriInfo ui, @QueryParam("start") String firstSubscriberQueuePath,
                                       @QueryParam("limit") @DefaultValue("10") int limit,
                                       @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        logger.info( "QueueSubscriberResource.executeGet: " + queuePath );

        QueueSet results = mq.getSubscribers( queuePath, firstSubscriberQueuePath, limit );

        return results;
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public QueueSet executePost( @Context UriInfo ui, Map<String, Object> body,
                                        @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        logger.info( "QueueSubscriberResource.executePost: " + queuePath );

        return executePut( ui, body, callback );
    }


    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public QueueSet executePut( @Context UriInfo ui, Map<String, Object> body,
                                       @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        logger.info( "QueueSubscriberResource.executePut: " + queuePath );

        Map<String, Object> json = body;
        if ( StringUtils.isNotBlank( subscriberPath ) ) {
            return mq.subscribeToQueue( queuePath, subscriberPath );
        }
        else if ( ( json != null ) && ( json.containsKey( "subscriber" ) ) ) {
            String subscriber = ( String ) json.get( "subscriber" );
            return mq.subscribeToQueue( queuePath, subscriber );
        }
        else if ( ( json != null ) && ( json.containsKey( "subscribers" ) ) ) {
            @SuppressWarnings("unchecked") List<String> subscribers = ( List<String> ) json.get( "subscribers" );
            return mq.addSubscribersToQueue( queuePath, subscribers );
        }

        return null;
    }


    @DELETE
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public QueueSet executeDelete( @Context UriInfo ui,
                                          @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        logger.info( "QueueSubscriberResource.executeDelete: " + queuePath );

        if ( StringUtils.isNotBlank( subscriberPath ) ) {
            return mq.unsubscribeFromQueue( queuePath, subscriberPath );
        }

        return null;
    }
}
