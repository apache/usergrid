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

public class Dbstats extends MongoCommand {

	@Override
	public OpReply execute(MongoChannelHandler handler,
			ChannelHandlerContext ctx, MessageEvent e, OpQuery opQuery) {
		OpReply reply = new OpReply(opQuery);
		reply.addDocument(map(entry("db", "local"), entry("collections", 3),
				entry("objects", 4), entry("avgObjSize", 53.0),
				entry("dataSize", 212), entry("storageSize", 16640),
				entry("numExtents", 3), entry("indexes", 1),
				entry("indexSize", 8192), entry("fileSize", 201326592),
				entry("ok", 1.0)));
		return reply;
	}

}
