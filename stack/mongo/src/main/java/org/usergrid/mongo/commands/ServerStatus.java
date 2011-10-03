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
import org.usergrid.mongo.MongoChannelHandler;
import org.usergrid.mongo.protocol.OpQuery;
import org.usergrid.mongo.protocol.OpReply;
import org.usergrid.utils.DateUtils;

public class ServerStatus extends MongoCommand {

	@Override
	public OpReply execute(MongoChannelHandler handler,
			ChannelHandlerContext ctx, MessageEvent e, OpQuery opQuery) {
		OpReply reply = new OpReply(opQuery);
		reply.addDocument(map(
				entry("host", "api.usergrid.com:27017"),
				entry("version", "1.8.1"),
				entry("process", "mongod"),
				entry("uptime", 1000.0),
				entry("uptimeEstimate", 1000.0),
				entry("localTime",
						map("$date", DateUtils.instance.iso8601DateNow())),
				entry("globalLock",
						map(entry("totalTime", 1.0),
								entry("lockTime", 1000.0),
								entry("ratio", 0.00001),
								entry("currentQueue",
										map(entry("total", 0),
												entry("readers", 0),
												entry("writers", 0))),
								entry("activeClients",
										map(entry("total", 0),
												entry("readers", 0),
												entry("writers", 0))))),
				entry("mem",
						map(entry("bits", 64), entry("resident", 20),
								entry("virtual", 2048),
								entry("supported", true), entry("mapped", 80))),
				entry("connections",
						map(entry("current", 1), entry("available", 256))),
				entry("extra_info", map("note", "fields vary by platform")),
				entry("indexCounters",
						map("btree",
								map(entry("accesses", 0), entry("hits", 0),
										entry("misses", 0), entry("resets", 0),
										entry("missRatio", 0.0)))),
				entry("backgroundFlushing",
						map(entry("flushes", 24),
								entry("total_ms", 256),
								entry("average_ms", 4.0),
								entry("last_ms", 16),
								entry("last_finished",
										map("$date", DateUtils.instance
												.iso8601DateNow())))),
				entry("cursors",
						map(entry("totalOpen", 0),
								entry("clientCursors_size", 0),
								entry("timedOut", 0))),
				entry("network",
						map(entry("bytesIn", 1024), entry("bytesOut", 1024),
								entry("numRequests", 32))),
				entry("opcounters",
						map(entry("insert", 0), entry("query", 1),
								entry("update", 0), entry("delete", 0),
								entry("getmore", 0), entry("command", 1))),
				entry("asserts",
						map(entry("regular", 0), entry("warning", 0),
								entry("msg", 0), entry("user", 0),
								entry("rollovers", 0))),
				entry("writeBacksQueued", false), entry("ok", 1.0)));
		return reply;
	}
}
