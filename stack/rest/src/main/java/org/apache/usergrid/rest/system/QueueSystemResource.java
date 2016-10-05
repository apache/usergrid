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

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.usergrid.corepersistence.asyncevents.AsyncEventService;
import org.apache.usergrid.persistence.qakka.MetricsService;
import org.apache.usergrid.persistence.qakka.QakkaFig;
import org.apache.usergrid.persistence.qakka.core.Queue;
import org.apache.usergrid.persistence.qakka.core.QueueManager;
import org.apache.usergrid.persistence.qakka.core.QueueMessageManager;
import org.apache.usergrid.persistence.qakka.core.impl.InMemoryQueue;
import org.apache.usergrid.persistence.qakka.core.impl.QueueManagerImpl;
import org.apache.usergrid.persistence.qakka.core.impl.QueueMessageManagerImpl;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.security.annotations.RequireSystemAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.*;

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


    @GET
    @RequireSystemAccess
    @Path("info")
    public ApiResponse getQueueInfo(
        @QueryParam("callback") @DefaultValue("callback") String callback ) {

        ApiResponse response = createApiResponse();
        response.setAction( "get queue info" );

        MetricsService metricsService = injector.getInstance( MetricsService.class );

        final DecimalFormat format = new DecimalFormat("##.###");
        final long nano = 1000000000;

        Map<String, Object> info = new HashMap<String, Object>() {{
            put( "name", "Queue Info" );
            try {
                put( "host", InetAddress.getLocalHost().getHostName() );
            } catch (UnknownHostException e) {
                put( "host", "unknown" );
            }
            SortedSet<String> names = metricsService.getMetricRegistry().getNames();
            for (String name : names) {
                Timer t = metricsService.getMetricRegistry().timer( name );
                put( name, new HashMap<String, Object>() {{
                    put( "count", ""            + t.getCount() );
                    put( "mean_rate", ""        + format.format( t.getMeanRate() ) );
                    put( "one_minute_rate", ""  + format.format( t.getOneMinuteRate() ) );
                    put( "five_minute_rate", "" + format.format( t.getFiveMinuteRate() ) );
                    put( "mean (s)", ""         + format.format( t.getSnapshot().getMean() / nano ) );
                    put( "min (s)", ""          + format.format( (double) t.getSnapshot().getMin() / nano ) );
                    put( "max (s)", ""          + format.format( (double) t.getSnapshot().getMax() / nano ) );
                }} );
            }
        }};

        QueueManager queueManager               = injector.getInstance( QueueManagerImpl.class );
        QueueMessageManager queueMessageManager = injector.getInstance( QueueMessageManagerImpl.class );
        InMemoryQueue inMemoryQueue             = injector.getInstance( InMemoryQueue.class );

        List queues = new ArrayList();
        final List<String> listOfQueues = queueManager.getListOfQueues();
        for ( String queueName : listOfQueues ) {

            Map<String, Object> queueInfo = new HashMap<>();

            queueInfo.put("name", queueName );
            queueInfo.put("depth", queueMessageManager.getQueueDepth( queueName ));
            queueInfo.put("inmemory", inMemoryQueue.size( queueName ));

            UUID newest = inMemoryQueue.getNewest( queueName );
            queueInfo.put("since", newest == null ? "null" : newest.timestamp());

            queues.add( queueInfo );
        }

        info.put("queues", queues);

        response.setProperty( "data", info );

        return response;
    }

}
