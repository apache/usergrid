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
import org.apache.usergrid.exception.NotImplementedException;
import org.apache.usergrid.mq.*;
import org.apache.usergrid.rest.AbstractContextResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
@Scope("prototype")
@Produces({
        MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
        "application/ecmascript", "text/jscript"
})
public class QueueResource extends AbstractContextResource {

    static final Logger logger = LoggerFactory.getLogger( QueueResource.class );

    QueueManager mq;
    String queuePath = "";


    public QueueResource() {
    }


    public QueueResource init( QueueManager mq, String queuePath ) {
        this.mq = mq;
        this.queuePath = queuePath;
        return this;
    }


    @Path("{subPath}")
    public QueueResource getSubPath( @Context UriInfo ui, @PathParam("subPath") String subPath ) throws Exception {

        logger.info( "QueueResource.getSubPath" );

        return getSubResource( QueueResource.class ).init( mq, queuePath + "/" + subPath );
    }


    @Path("subscribers")
    public QueueSubscriberResource getSubscribers( @Context UriInfo ui ) throws Exception {

        logger.info( "QueueResource.getSubscribers" );

        return getSubResource( QueueSubscriberResource.class ).init( mq, queuePath );
    }


    @Path("subscriptions")
    public QueueSubscriptionResource getSubscriptions( @Context UriInfo ui ) throws Exception {

        logger.info( "QueueResource.getSubscriptions" );

        return getSubResource( QueueSubscriptionResource.class ).init( mq, queuePath );
    }


    @Path("properties")
    @GET
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Queue getProperties( @Context UriInfo ui,
                                          @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        logger.info( "QueueResource.getProperties" );

        return mq.getQueue( queuePath );
    }


    @Path("properties")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Queue putProperties( @Context UriInfo ui, Map<String, Object> json,
                                          @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        logger.info( "QueueResource.putProperties" );

        return mq.updateQueue( queuePath, json );
    }


    @GET
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Object executeGet( @Context UriInfo ui, @QueryParam("start") String firstQueuePath,
                                       @QueryParam("limit") @DefaultValue("10") int limit,
                                       @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        if ( StringUtils.isNotBlank( queuePath ) ) {
            logger.info( "QueueResource.executeGet: " + queuePath );

            QueueQuery query = QueueQuery.fromQueryParams( ui.getQueryParameters() );
            QueueResults results = mq.getFromQueue( queuePath, query );
            return results;
        }

        logger.info( "QueueResource.executeGet" );

        return  mq.getQueues( firstQueuePath, limit );
    }


    @SuppressWarnings("unchecked")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public QueueResults executePost( @Context UriInfo ui, Object body,
                                        @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        logger.info( "QueueResource.executePost: " + queuePath );
        Object json = body;

        if ( json instanceof Map ) {
            return new QueueResults( mq.postToQueue( queuePath, new Message( ( Map<String, Object> ) json ) ));
        }
        else if ( json instanceof List ) {
            return new QueueResults( mq.postToQueue( queuePath, Message.fromList( ( List<Map<String, Object>> ) json ) ) );
        }

        return null;
    }


    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Map<String, Object> executePut( @Context UriInfo ui, Map<String, Object> json,
                                       @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        logger.info( "QueueResource.executePut: " + queuePath );

        Map<String, Object> results = new HashMap<String, Object>();

        return results;
    }


    @DELETE
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Queue executeDelete(
            @Context UriInfo ui, @QueryParam("callback") @DefaultValue("callback") String callback ) throws Exception {
        throw new NotImplementedException( "Queue delete is not implemented yet" );
    }


    @Path("transactions")
    public QueueTransactionsResource getTransactions( @Context UriInfo ui ) throws Exception {

        logger.info( "QueueResource.getSubscriptions" );

        return getSubResource( QueueTransactionsResource.class ).init( mq, queuePath );
    }
}
