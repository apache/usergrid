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

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.springframework.util.StringUtils;
import org.usergrid.mongo.MongoChannelHandler;
import org.usergrid.mongo.protocol.OpQuery;
import org.usergrid.mongo.protocol.OpReply;

public abstract class MongoCommand {

	private static final Logger logger = LoggerFactory.getLogger(MongoCommand.class);

	static ConcurrentHashMap<String, MongoCommand> commands = new ConcurrentHashMap<String, MongoCommand>();

	@SuppressWarnings("unchecked")
	public static MongoCommand getCommand(String commandName) {
		MongoCommand command = commands.get(commandName);
		if (command != null) {
			return command;
		}

		String clazz = "org.usergrid.mongo.commands."
				+ StringUtils.capitalize(commandName);

		Class<MongoCommand> cls = null;

		try {
			cls = (Class<MongoCommand>) Class.forName(clazz);
		} catch (ClassNotFoundException e) {
			logger.error("Couldn't find command class", e);
		}
    logger.debug("using MongoCommand class {}", clazz);
		try {
			if (cls != null) {
				command = cls.newInstance();
			}
		} catch (Exception e) {
			logger.error("Couldn't find instantiate class", e);
		}

		if (command != null) {
			MongoCommand oldCommand = commands
					.putIfAbsent(commandName, command);
			if (oldCommand != null) {
				command = oldCommand;
			}
		} else {
			logger.warn("Mongo command handler not found for " + commandName);
		}

		return command;
	}

	public abstract OpReply execute(MongoChannelHandler handler,
			ChannelHandlerContext ctx, MessageEvent e, OpQuery opQuery);

}
