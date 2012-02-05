/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Stack.
 * 
 * Usergrid Stack is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * 
 * Usergrid Stack is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License along
 * with Usergrid Stack. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU AGPL version 3 section 7
 * 
 * Linking Usergrid Stack statically or dynamically with other modules is making
 * a combined work based on Usergrid Stack. Thus, the terms and conditions of the
 * GNU General Public License cover the whole combination.
 * 
 * In addition, as a special exception, the copyright holders of Usergrid Stack
 * give you permission to combine Usergrid Stack with free software programs or
 * libraries that are released under the GNU LGPL and with independent modules
 * that communicate with Usergrid Stack solely through:
 * 
 *   - Classes implementing the org.usergrid.services.Service interface
 *   - Apache Shiro Realms and Filters
 *   - Servlet Filters and JAX-RS/Jersey Filters
 * 
 * You may copy and distribute such a system following the terms of the GNU AGPL
 * for Usergrid Stack and the licenses of the other code concerned, provided that
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
import org.usergrid.mq.Message;
import org.usergrid.mq.QueueManager;
import org.usergrid.mq.QueueQuery;
import org.usergrid.mq.QueueResults;
import org.usergrid.rest.AbstractContextResource;
import org.usergrid.rest.applications.ServiceResource;

import com.sun.jersey.api.json.JSONWithPadding;

@Produces({ MediaType.APPLICATION_JSON, "application/javascript",
		"application/x-javascript", "text/ecmascript",
		"application/ecmascript", "text/jscript" })
public class QueueResource extends AbstractContextResource {

	static final Logger logger = LoggerFactory.getLogger(QueueResource.class);

	QueueManager mq;
	String queuePath = "";

	public QueueResource(ServiceResource service, QueueManager mq)
			throws Exception {
		super(service);

		this.mq = mq;
	}

	public QueueResource(QueueResource parent, QueueManager mq, String queuePath)
			throws Exception {
		super(parent);

		this.mq = mq;
		this.queuePath = queuePath;

	}

	@Path("{subPath}")
	public QueueResource getSubPath(@Context UriInfo ui,
			@PathParam("subPath") String subPath) throws Exception {

		logger.info("QueueResource.getSubPath");

		return new QueueResource(this, mq, queuePath + "/" + subPath);
	}

	@Path("subscribers")
	public QueueSubscriberResource getSubscribers(@Context UriInfo ui)
			throws Exception {

		logger.info("QueueResource.getSubscribers");

		return new QueueSubscriberResource(this, mq, queuePath);
	}

	@Path("subscriptions")
	public QueueSubscriptionResource getSubscriptions(@Context UriInfo ui)
			throws Exception {

		logger.info("QueueResource.getSubscriptions");

		return new QueueSubscriptionResource(this, mq, queuePath);
	}

	@Path("properties")
	@GET
	public JSONWithPadding getProperties(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		logger.info("QueueResource.getProperties");

		return new JSONWithPadding(mq.getQueue(queuePath), callback);
	}

	@Path("properties")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public JSONWithPadding putProperties(@Context UriInfo ui,
			Map<String, Object> json,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		logger.info("QueueResource.putProperties");

		return new JSONWithPadding(mq.updateQueue(queuePath, json), callback);
	}

	@GET
	public JSONWithPadding executeGet(@Context UriInfo ui,
			@QueryParam("start") String firstQueuePath,
			@QueryParam("limit") @DefaultValue("10") int limit,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		if (StringUtils.isNotBlank(queuePath)) {
			logger.info("QueueResource.executeGet: " + queuePath);

			QueueQuery query = QueueQuery.fromQueryParams(ui
					.getQueryParameters());
			QueueResults results = mq.getFromQueue(queuePath, query);
			return new JSONWithPadding(results, callback);
		}

		logger.info("QueueResource.executeGet");

		return new JSONWithPadding(mq.getQueues(firstQueuePath, limit),
				callback);
	}

	@SuppressWarnings("unchecked")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public JSONWithPadding executePost(@Context UriInfo ui, Object json,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		logger.info("QueueResource.executePost: " + queuePath);

		if (json instanceof Map) {
			return new JSONWithPadding(new QueueResults(mq.postToQueue(
					queuePath, new Message((Map<String, Object>) json))),
					callback);
		} else if (json instanceof List) {
			return new JSONWithPadding(new QueueResults(mq.postToQueue(
					queuePath,
					Message.fromList((List<Map<String, Object>>) json))),
					callback);
		}

		return null;
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public JSONWithPadding executePut(@Context UriInfo ui,
			Map<String, Object> json,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		logger.info("QueueResource.executePut: " + queuePath);

		Map<String, Object> results = new HashMap<String, Object>();

		return new JSONWithPadding(results, callback);
	}

	@DELETE
	public JSONWithPadding executeDelete(@Context UriInfo ui,
			@QueryParam("callback") @DefaultValue("callback") String callback)
			throws Exception {

		logger.info("QueueResource.executeDelete: " + queuePath);

		Map<String, Object> results = new HashMap<String, Object>();

		return new JSONWithPadding(results, callback);
	}
}
