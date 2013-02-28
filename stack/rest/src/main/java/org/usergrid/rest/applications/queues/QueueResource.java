/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.rest.applications.queues;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.usergrid.exception.NotImplementedException;
import org.usergrid.mq.Message;
import org.usergrid.mq.QueueManager;
import org.usergrid.mq.QueueQuery;
import org.usergrid.mq.QueueResults;
import org.usergrid.rest.AbstractContextResource;

import com.sun.jersey.api.json.JSONWithPadding;
import com.sun.jersey.core.provider.EntityHolder;

@Component
@Scope("prototype")
@Produces({ MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
    "application/ecmascript", "text/jscript" })
public class QueueResource extends AbstractContextResource {

  static final Logger logger = LoggerFactory.getLogger(QueueResource.class);

  QueueManager mq;
  String queuePath = "";

  public QueueResource() {
  }

  public QueueResource init(QueueManager mq, String queuePath) {
    this.mq = mq;
    this.queuePath = queuePath;
    return this;
  }

  @Path("{subPath}")
  public QueueResource getSubPath(@Context UriInfo ui, @PathParam("subPath") String subPath) throws Exception {

    logger.info("QueueResource.getSubPath");

    return getSubResource(QueueResource.class).init(mq, queuePath + "/" + subPath);
  }

  @Path("subscribers")
  public QueueSubscriberResource getSubscribers(@Context UriInfo ui) throws Exception {

    logger.info("QueueResource.getSubscribers");

    return getSubResource(QueueSubscriberResource.class).init(mq, queuePath);
  }

  @Path("subscriptions")
  public QueueSubscriptionResource getSubscriptions(@Context UriInfo ui) throws Exception {

    logger.info("QueueResource.getSubscriptions");

    return getSubResource(QueueSubscriptionResource.class).init(mq, queuePath);
  }

  @Path("properties")
  @GET
  public JSONWithPadding getProperties(@Context UriInfo ui,
      @QueryParam("callback") @DefaultValue("callback") String callback) throws Exception {

    logger.info("QueueResource.getProperties");

    return new JSONWithPadding(mq.getQueue(queuePath), callback);
  }

  @Path("properties")
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  public JSONWithPadding putProperties(@Context UriInfo ui, Map<String, Object> json,
      @QueryParam("callback") @DefaultValue("callback") String callback) throws Exception {

    logger.info("QueueResource.putProperties");

    return new JSONWithPadding(mq.updateQueue(queuePath, json), callback);
  }

  @GET
  public JSONWithPadding executeGet(@Context UriInfo ui, @QueryParam("start") String firstQueuePath,
      @QueryParam("limit") @DefaultValue("10") int limit,
      @QueryParam("callback") @DefaultValue("callback") String callback) throws Exception {

    if (StringUtils.isNotBlank(queuePath)) {
      logger.info("QueueResource.executeGet: " + queuePath);

      QueueQuery query = QueueQuery.fromQueryParams(ui.getQueryParameters());
      QueueResults results = mq.getFromQueue(queuePath, query);
      return new JSONWithPadding(results, callback);
    }

    logger.info("QueueResource.executeGet");

    return new JSONWithPadding(mq.getQueues(firstQueuePath, limit), callback);
  }

  @SuppressWarnings("unchecked")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public JSONWithPadding executePost(@Context UriInfo ui, EntityHolder<Object> body,
      @QueryParam("callback") @DefaultValue("callback") String callback) throws Exception {

    logger.info("QueueResource.executePost: " + queuePath);
    Object json = body.getEntity();

    if (json instanceof Map) {
      return new JSONWithPadding(new QueueResults(mq.postToQueue(queuePath, new Message((Map<String, Object>) json))),
          callback);
    } else if (json instanceof List) {
      return new JSONWithPadding(new QueueResults(mq.postToQueue(queuePath,
          Message.fromList((List<Map<String, Object>>) json))), callback);
    }

    return null;
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  public JSONWithPadding executePut(@Context UriInfo ui, Map<String, Object> json,
      @QueryParam("callback") @DefaultValue("callback") String callback) throws Exception {

    logger.info("QueueResource.executePut: " + queuePath);

    Map<String, Object> results = new HashMap<String, Object>();

    return new JSONWithPadding(results, callback);
  }

  @DELETE
  public JSONWithPadding executeDelete(@Context UriInfo ui,
      @QueryParam("callback") @DefaultValue("callback") String callback) throws Exception {
    throw new NotImplementedException("Queue delete is not implemented yet");
  }
  
  @Path("transactions")
  public QueueTransactionsResource getTransactions(@Context UriInfo ui) throws Exception {

    logger.info("QueueResource.getSubscriptions");

    return getSubResource(QueueTransactionsResource.class).init(mq, queuePath);
  }
}
