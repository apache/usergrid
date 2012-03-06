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
