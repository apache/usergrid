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
