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


import java.util.UUID;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.usergrid.rest.security.annotations.CheckPermissionsForPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.apache.usergrid.mq.QueueManager;
import org.apache.usergrid.mq.QueueQuery;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.rest.AbstractContextResource;

import com.sun.jersey.api.json.JSONWithPadding;

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


    @CheckPermissionsForPath
    @Path("{id}")
    @PUT
    public JSONWithPadding updateTransaction( @Context UriInfo ui, @PathParam("id") UUID transactionId,
                                              @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        QueueQuery query = QueueQuery.fromQueryParams( ui.getQueryParameters() );

        UUID newTransactionId = mq.renewTransaction( queuePath, transactionId, query );

        return new JSONWithPadding( Results.fromData( hashMap( "transaction", newTransactionId ) ), callback );
    }


    @CheckPermissionsForPath
    @Path("{id}")
    @DELETE
    public JSONWithPadding removeTransaction( @Context UriInfo ui, @PathParam("id") UUID transactionId,
                                              @QueryParam("callback") @DefaultValue("callback") String callback )
            throws Exception {

        QueueQuery query = QueueQuery.fromQueryParams( ui.getQueryParameters() );


        mq.deleteTransaction( this.queuePath, transactionId, query );

        return new JSONWithPadding( Results.fromData( hashMap( "transaction", transactionId ) ), callback );
    }
}
