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
import org.apache.usergrid.mq.QueueManager;
import org.apache.usergrid.mq.QueueQuery;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.rest.AbstractContextResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.UUID;

import static org.apache.usergrid.utils.MapUtils.hashMap;


@Component
@Scope("prototype")
@Produces({
        MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
        "application/ecmascript", "text/jscript"
})
public class QueueTransactionsResource extends AbstractContextResource {

    static final Logger logger = LoggerFactory.getLogger( QueueTransactionsResource.class );

    QueueManager mq;
    String queuePath = "";
    String subscriptionPath = "";


    public QueueTransactionsResource() {
    }


    public QueueTransactionsResource init( QueueManager mq, String queuePath ) {
        this.mq = mq;
        this.queuePath = queuePath;
        return this;
    }


    @Path("{id}")
    @PUT
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Results updateTransaction( @Context UriInfo ui, @PathParam("id") UUID transactionId,
                                              @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        QueueQuery query = QueueQuery.fromQueryParams( ui.getQueryParameters() );

        UUID newTransactionId = mq.renewTransaction( queuePath, transactionId, query );

        return Results.fromData( hashMap( "transaction", newTransactionId ) );
    }


    @Path("{id}")
    @DELETE
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Results removeTransaction( @Context UriInfo ui, @PathParam("id") UUID transactionId,
                                              @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        QueueQuery query = QueueQuery.fromQueryParams( ui.getQueryParameters() );


        mq.deleteTransaction( this.queuePath, transactionId, query );

        return Results.fromData( hashMap( "transaction", transactionId ) );
    }
}
