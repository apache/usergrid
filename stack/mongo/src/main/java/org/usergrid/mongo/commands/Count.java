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
package org.usergrid.mongo.commands;

import static org.usergrid.utils.MapUtils.entry;
import static org.usergrid.utils.MapUtils.map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.mongo.MongoChannelHandler;
import org.usergrid.mongo.protocol.OpQuery;
import org.usergrid.mongo.protocol.OpReply;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.Identifier;
import org.usergrid.persistence.Results;
import org.usergrid.security.shiro.utils.SubjectUtils;

public class Count extends MongoCommand {

	private static final Logger logger = LoggerFactory.getLogger(Count.class);

	@Override
	public OpReply execute(MongoChannelHandler handler,
			ChannelHandlerContext ctx, MessageEvent e, OpQuery opQuery) {
		ApplicationInfo application = SubjectUtils.getApplication(Identifier
				.fromName(opQuery.getDatabaseName()));
		if (application == null) {
			OpReply reply = new OpReply(opQuery);
			return reply;
		}
		EntityManager em = handler.getEmf().getEntityManager(application.getId());
		OpReply reply = new OpReply(opQuery);
		try {
			Results results = em.getCollection(em.getApplicationRef(),
					(String) opQuery.getQuery().get("count"), null, 100000,
					Results.Level.IDS, false);
			reply.addDocument(map(entry("n", results.size() * 1.0),
					entry("ok", 1.0)));
		} catch (Exception ex) {
			logger.error("Unable to retrieve collections", ex);
		}
		return reply;
	}

}
