/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.qakka.api;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import org.apache.usergrid.persistence.qakka.MetricsService;
import org.apache.usergrid.persistence.qakka.core.*;
import org.apache.usergrid.persistence.qakka.serialization.sharding.ShardCounterSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


@Path("queues")
public class QueueResource {
    private static final Logger logger = LoggerFactory.getLogger( QueueResource.class );

    private final QueueManager queueManager;
    private final QueueMessageManager queueMessageManager;
    private final MetricsService            metricsService;
    private final URIStrategy               uriStrategy;
    private final Regions regions;
    private final ShardCounterSerialization shardCounterSerialization;


    @Inject
    public QueueResource(
            QueueManager              queueManager,
            QueueMessageManager       queueMessageManager,
            MetricsService            metricsService,
            URIStrategy               uriStrategy,
            Regions                   regions,
            ShardCounterSerialization shardCounterSerialization ) {

        this.queueManager              = queueManager;
        this.queueMessageManager       = queueMessageManager;
        this.metricsService            = metricsService;
        this.uriStrategy               = uriStrategy;
        this.regions                   = regions;
        this.shardCounterSerialization = shardCounterSerialization;
    }


    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response createQueue(Queue queue ) throws Exception {

        Preconditions.checkArgument(queue != null, "Queue configuration is required");
        Preconditions.checkArgument(!QakkaUtils.isNullOrEmpty(queue.getName()), "Queue name is required");

        queueManager.createQueue(queue);

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setQueues( Collections.singletonList(queue) );
        return Response.created( uriStrategy.queueURI( queue.getName() )).entity(apiResponse).build();
    }


    @PUT
    @Path( "{queueName}/config" )
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateQueueConfig( @PathParam("queueName") String queueName, Queue queue) {

        Preconditions.checkArgument(!QakkaUtils.isNullOrEmpty(queueName), "Queue name is required");
        Preconditions.checkArgument(queue != null, "Queue configuration is required");

        queue.setName(queueName);
        queueManager.updateQueueConfig(queue);

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setQueues( Collections.singletonList(queue) );
        return Response.ok().entity(apiResponse).build();
    }


    @DELETE
    @Path( "{queueName}" )
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteQueue( @PathParam("queueName") String queueName,
                                 @QueryParam("confirm") @DefaultValue("false") Boolean confirmedParam) {

        Preconditions.checkArgument(!QakkaUtils.isNullOrEmpty(queueName), "Queue name is required");
        Preconditions.checkArgument(confirmedParam != null, "Confirm parameter is required");

        ApiResponse apiResponse = new ApiResponse();

        if ( confirmedParam ) {
            queueManager.deleteQueue( queueName );
            return Response.ok().entity( apiResponse ).build();
        }

        apiResponse.setMessage( "confirm parameter must be true" );
        return Response.status( Response.Status.BAD_REQUEST ).entity( apiResponse ).build();
    }


    @GET
    @Path( "{queueName}/config" )
    @Produces({MediaType.APPLICATION_JSON})
    public Response getQueueConfig( @PathParam("queueName") String queueName) {

        Preconditions.checkArgument(!QakkaUtils.isNullOrEmpty(queueName), "Queue name is required");

        ApiResponse apiResponse = new ApiResponse();
        Queue queue = queueManager.getQueueConfig( queueName );
        if ( queue != null ) {
            apiResponse.setQueues( Collections.singletonList(queue) );
            return Response.ok().entity(apiResponse).build();
        }
        return Response.status( Response.Status.NOT_FOUND ).build();
    }


    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public List<String> getListOfQueues() {

        // TODO: create design to handle large number of queues, e.g. paging and/or hierarchy of queues

        // TODO: create design to support multi-tenant usage, authentication, etc.

        return queueManager.getListOfQueues();
    }


    @GET
    @Path( "{queueName}/stats" )
    @Produces({MediaType.APPLICATION_JSON})
    public Response getQueueStats( @PathParam("queueName") String queueName) throws Exception {
        // TODO: implement GET /queues/{queueName}/stats
        throw new UnsupportedOperationException();
    }


    Long convertDelayParameter(String delayParam) {
        Long delayMs = 0L;
        if (!QakkaUtils.isNullOrEmpty(delayParam)) {
            switch (delayParam.toUpperCase()) {
                case "NONE":
                case "":
                    delayMs = 0L;
                    break;
                default:
                    try {
                        delayMs = Long.parseLong(delayParam);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid delay parameter");
                    }
                    break;
            }
        }
        return delayMs;
    }

    Long convertExpirationParameter(String expirationParam) throws IllegalArgumentException {
        Long expirationSecs = null;
        if (!QakkaUtils.isNullOrEmpty(expirationParam)) {
            switch (expirationParam.toUpperCase()) {
                case "NEVER":
                case "":
                    expirationSecs = null;
                    break;
                default:
                    try {
                        expirationSecs = Long.parseLong(expirationParam);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid expiration parameter");
                    }
                    break;
            }
        }
        return expirationSecs;
    }


    /**
     * Send a queue message with a JSON payload.
     *
     * @param queueName         Name of queue to target (queue must exist)
     * @param regionsParam      Comma-separated list of regions to send to
     * @param delayParam        Delay (ms) before sending message (not yet supported)
     * @param expirationParam   Time (ms) after which message will expire (not yet supported)
     * @param messageBody       JSON payload in string form
     */
    @POST
    @Path( "{queueName}/messages" )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendMessageJson(
            @PathParam("queueName")                     String queueName,
            @QueryParam("regions" )   @DefaultValue("") String regionsParam,
            @QueryParam("delay")      @DefaultValue("") String delayParam,
            @QueryParam("expiration") @DefaultValue("") String expirationParam,
                                                        String messageBody) throws Exception {

        return sendMessage( queueName, regionsParam, delayParam, expirationParam,
                MediaType.APPLICATION_JSON, ByteBuffer.wrap( messageBody.getBytes() ) );
    }


    /**
     * Send a queue message with a binary data payload.
     *
     * @param queueName         Name of queue to target (queue must exist)
     * @param regionsParam      Comma-separated list of regions to send to
     * @param delayParam        Delay (ms) before sending message (not yet supported)
     * @param expirationParam   Time (ms) after which message will expire (not yet supported)
     * @param actualContentType Content type of messageBody data (if not application/octet-stream)
     * @param messageBody       Binary data that is the payload of the queue message
     */
    @POST
    @Path( "{queueName}/messages" )
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendMessageBinary(
            @PathParam("queueName")                     String queueName,
            @QueryParam("regions" )   @DefaultValue("") String regionsParam,
            @QueryParam("delay")      @DefaultValue("") String delayParam,
            @QueryParam("expiration") @DefaultValue("") String expirationParam,
            @QueryParam("contentType")                  String actualContentType,
                                                        byte[] messageBody) throws Exception {

        String contentType = actualContentType != null ? actualContentType : MediaType.APPLICATION_OCTET_STREAM;

        return sendMessage( queueName, regionsParam, delayParam, expirationParam,
                contentType, ByteBuffer.wrap( messageBody ) );
    }


    private Response sendMessage( String queueName,
                                   String regionsParam,
                                   String delayParam,
                                   String expirationParam,
                                   String contentType,
                                   ByteBuffer byteBuffer) {

        Timer.Context timer = metricsService.getMetricRegistry().timer( MetricsService.SEND_TIME_TOTAL ).time();
        try {

            Preconditions.checkArgument( !QakkaUtils.isNullOrEmpty( queueName ), "Queue name is required" );

            // if regions, delay or expiration are empty string, would get the defaults from the queue
            if (regionsParam.equals( "" )) {
                regionsParam = Regions.LOCAL;
            }

            Long delayMs = convertDelayParameter( delayParam );

            Long expirationSecs = convertExpirationParameter( expirationParam );

            List<String> regionList = regions.getRegions( regionsParam );

            queueMessageManager.sendMessages( queueName, regionList, delayMs, expirationSecs,
                    contentType, byteBuffer );

            ApiResponse apiResponse = new ApiResponse();
            apiResponse.setCount( 1 );
            return Response.ok().entity( apiResponse ).build();

        } finally {
            timer.close();
        }
    }


    @GET
    @Path( "{queueName}/messages" )
    @Produces({MediaType.APPLICATION_JSON})
    public Response getNextMessages( @PathParam("queueName") String queueName,
                                     @QueryParam("count") @DefaultValue("1") String countParam) throws Exception {

        Timer.Context timer = metricsService.getMetricRegistry().timer( MetricsService.GET_TIME_TOTAL ).time();
        try {

            Preconditions.checkArgument( !QakkaUtils.isNullOrEmpty( queueName ), "Queue name is required" );

            int count = 1;
            try {
                count = Integer.parseInt( countParam );
            } catch (Exception e) {
                throw new IllegalArgumentException( "Invalid count parameter" );
            }
            if (count <= 0) {
                // invalid count
                throw new IllegalArgumentException( "Count must be >= 1" );
            }

            List<QueueMessage> messages = queueMessageManager.getNextMessages( queueName, count );

            ApiResponse apiResponse = new ApiResponse();

            if (messages != null && !messages.isEmpty()) {
                apiResponse.setQueueMessages( messages );

            } else { // always return queueMessages field
                apiResponse.setQueueMessages( Collections.EMPTY_LIST );
            }
            apiResponse.setCount( apiResponse.getQueueMessages().size() );
            return Response.ok().entity( apiResponse ).build();

        } finally {
            timer.close();
        }
    }


    @DELETE
    @Path( "{queueName}/messages/{queueMessageId}" )
    @Produces({MediaType.APPLICATION_JSON})
    public Response ackMessage( @PathParam("queueName") String queueName,
                                @PathParam("queueMessageId") String queueMessageId) throws Exception {

        Timer.Context timer = metricsService.getMetricRegistry().timer( MetricsService.ACK_TIME_TOTAL ).time();
        try {

            Preconditions.checkArgument( !QakkaUtils.isNullOrEmpty( queueName ), "Queue name is required" );

            UUID messageUuid;
            try {
                messageUuid = UUID.fromString( queueMessageId );
            } catch (Exception e) {
                throw new IllegalArgumentException( "Invalid queue message UUID" );
            }
            queueMessageManager.ackMessage( queueName, messageUuid );

            ApiResponse apiResponse = new ApiResponse();
            return Response.ok().entity( apiResponse ).build();

        } finally {
            timer.close();
        }
    }


    @GET
    @Path( "{queueName}/data/{queueMessageId}" )
    public Response getMessageData(
            @PathParam("queueName") String queueName,
            @PathParam("queueMessageId") String queueMessageIdParam ) {

        Preconditions.checkArgument(!QakkaUtils.isNullOrEmpty(queueName), "Queue name is required");

        UUID queueMessageId;
        try {
            queueMessageId = UUID.fromString(queueMessageIdParam);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Invalid queue message UUID");
        }

        QueueMessage message = queueMessageManager.getMessage( queueName, queueMessageId );
        if ( message == null ) {
            throw new NotFoundException(
                    "Message not found for queueName: " + queueName + " queue message id: " + queueMessageId );
        }

        ByteBuffer messageData = queueMessageManager.getMessageData( message.getMessageId() );
        if ( messageData == null ) {
            throw new NotFoundException( "Message data not found queueName: " + queueName
                    + " queue message id: " + queueMessageId + " message id: " + message.getMessageId() );
        }

        ByteBufferBackedInputStream input = new ByteBufferBackedInputStream( messageData );

        StreamingOutput stream = output -> {
            try {
                ByteStreams.copy(input, output);
            } catch (Exception e) {
                throw new WebApplicationException(e);
            }
        };

        return Response.ok( stream ).header( "Content-Type", message.getContentType() ).build();
    }


//    @PUT
//    @Path( "{queueName}/messages/{queueMessageId}" )
//    @Produces({MediaType.APPLICATION_JSON})
//    public Response requeueMessage( @PathParam("queueName") String queueName,
//                                    @PathParam("queueMessageId") String queueMessageIdParam,
//                                    @QueryParam("delay") @DefaultValue("") String delayParam) throws Exception {
//
//        Preconditions.checkArgument(!QakkaUtils.isNullOrEmpty(queueName), "Queue name is required");
//
//        UUID queueMessageId;
//        try {
//            queueMessageId = UUID.fromString(queueMessageIdParam);
//        }
//        catch (Exception e) {
//            throw new IllegalArgumentException("Invalid message UUID");
//        }
//        Long delayMs = convertDelayParameter(delayParam);
//
//        queueMessageManager.requeueMessage(queueName, queueMessageId, delayMs);
//
//        ApiResponse apiResponse = new ApiResponse();
//        return Response.ok().entity(apiResponse).build();
//    }
//
//
//    @DELETE
//    @Path( "{queueName}/messages" )
//    @Produces({MediaType.APPLICATION_JSON})
//    public Response clearMessages( @PathParam("queueName") String queueName,
//                                   @QueryParam("confirm") @DefaultValue("false") Boolean confirmed) throws Exception {
//
//        // TODO: implement DELETE /queues/{queueName}/messages"
//        throw new UnsupportedOperationException();
//    }

}
