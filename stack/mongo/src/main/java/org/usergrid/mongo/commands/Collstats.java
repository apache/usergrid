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
package org.usergrid.mongo.commands;

import static org.usergrid.utils.MapUtils.entry;
import static org.usergrid.utils.MapUtils.map;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.mongo.MongoChannelHandler;
import org.usergrid.mongo.protocol.OpQuery;
import org.usergrid.mongo.protocol.OpReply;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.Identifier;
import org.usergrid.security.shiro.utils.SubjectUtils;

public class Collstats extends MongoCommand {

	@Override
	public OpReply execute(MongoChannelHandler handler,
			ChannelHandlerContext ctx, MessageEvent e, OpQuery opQuery) {
		ApplicationInfo application = SubjectUtils.getApplication(Identifier
				.fromName(opQuery.getDatabaseName()));
		OpReply reply = new OpReply(opQuery);
		if (application == null) {
			return reply;
		}
		EntityManager em = handler.getEmf().getEntityManager(
				application.getId());
		String collectionName = (String) opQuery.getQuery().get("collstats");
		long count = 0;
		try {
			count = em.getApplicationCollectionSize(collectionName);
		} catch (Exception e1) {
		}
		reply.addDocument(map(
				entry("ns", opQuery.getDatabaseName() + "." + collectionName),
				entry("count", count), entry("size", count * 40),
				entry("avgObjSize", 40.0), entry("storageSize", 8192),
				entry("numExtents", 1), entry("nindexes", 1),
				entry("lastExtentSize", 8192), entry("paddingFactor", 1.0),
				entry("flags", 1), entry("totalIndexSize", 8192),
				entry("indexSizes", map("_id_", 8192)), entry("ok", 1.0)));
		return reply;
	}

}
