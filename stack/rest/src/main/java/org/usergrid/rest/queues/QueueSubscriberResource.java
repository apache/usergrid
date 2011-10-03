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
package org.usergrid.rest.queues;

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
import org.apache.log4j.Logger;
import org.usergrid.mq.QueueManager;
import org.usergrid.mq.QueueSet;
import org.usergrid.rest.AbstractContextResource;

@Produces(MediaType.APPLICATION_JSON)
public class QueueSubscriberResource extends AbstractContextResource {

	static final Logger logger = Logger
			.getLogger(QueueSubscriberResource.class);

	QueueManager mq;
	String queuePath = "";
	String subscriberPath = "";

	public QueueSubscriberResource(QueueResource parent, QueueManager mq,
			String queuePath) throws Exception {
		super(parent);

		this.mq = mq;
		this.queuePath = queuePath;

	}

	public QueueSubscriberResource(QueueSubscriberResource parent,
			QueueManager mq, String queuePath, String subscriberPath)
			throws Exception {
		super(parent);

		this.mq = mq;
		this.queuePath = queuePath;
		this.subscriberPath = subscriberPath;
	}

	@Path("{subPath}")
	public QueueSubscriberResource getSubPath(@Context UriInfo ui,
			@PathParam("subPath") String subPath) throws Exception {

		logger.info("QueueSubscriberResource.getSubPath");

		return new QueueSubscriberResource(this, mq, queuePath, subscriberPath
				+ "/" + subPath);
	}

	@GET
	public QueueSet executeGet(@Context UriInfo ui,
			@QueryParam("start") String firstSubscriberQueuePath,
			@QueryParam("limit") @DefaultValue("10") int limit)
			throws Exception {

		logger.info("QueueSubscriberResource.executeGet: " + queuePath);

		QueueSet results = mq.getSubscribers(queuePath,
				firstSubscriberQueuePath, limit);

		return results;
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public QueueSet executePost(@Context UriInfo ui, Map<String, Object> json)
			throws Exception {

		logger.info("QueueSubscriberResource.executePost: " + queuePath);

		return executePut(ui, json);
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public QueueSet executePut(@Context UriInfo ui, Map<String, Object> json)
			throws Exception {

		logger.info("QueueSubscriberResource.executePut: " + queuePath);

		if (StringUtils.isNotBlank(subscriberPath)) {
			return mq.subscribeToQueue(queuePath, subscriberPath);
		} else if ((json != null) && (json.containsKey("subscriber"))) {
			String subscriber = (String) json.get("subscriber");
			return mq.subscribeToQueue(queuePath, subscriber);
		} else if ((json != null) && (json.containsKey("subscribers"))) {
			@SuppressWarnings("unchecked")
			List<String> subscribers = (List<String>) json.get("subscribers");
			return mq.addSubscribersToQueue(queuePath, subscribers);
		}

		return null;
	}

	@DELETE
	public QueueSet executeDelete(@Context UriInfo ui) throws Exception {

		logger.info("QueueSubscriberResource.executeDelete: " + queuePath);

		if (StringUtils.isNotBlank(subscriberPath)) {
			return mq.unsubscribeFromQueue(queuePath, subscriberPath);
		}

		return null;
	}

}
