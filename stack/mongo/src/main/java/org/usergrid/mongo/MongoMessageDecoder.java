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
package org.usergrid.mongo;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.mongo.protocol.Message;
import org.usergrid.mongo.protocol.OpDelete;
import org.usergrid.mongo.protocol.OpGetMore;
import org.usergrid.mongo.protocol.OpInsert;
import org.usergrid.mongo.protocol.OpKillCursors;
import org.usergrid.mongo.protocol.OpMsg;
import org.usergrid.mongo.protocol.OpQuery;
import org.usergrid.mongo.protocol.OpReply;
import org.usergrid.mongo.protocol.OpUpdate;

public class MongoMessageDecoder extends FrameDecoder {

	private static final Logger logger = LoggerFactory
			.getLogger(MongoMessageDecoder.class);

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel,
			ChannelBuffer buf) throws Exception {

		if (buf.readableBytes() < 4) {
			logger.info("Needed at least 4 bytes, only " + buf.readableBytes()
					+ " available");
			return null;
		}

		// logger.info("Mongo message decoding...");

		int length = buf.getInt(buf.readerIndex());

		if (length < 0) {
			logger.info("Negative length " + length);
			return null;
		}

		if (buf.readableBytes() < length) {
			logger.info("Needed " + length + " bytes, only "
					+ buf.readableBytes() + " available");
			return null;
		}

		// logger.info("Attempting to read " + length + " bytes");
		ChannelBuffer frame = buf.readSlice(length);

		int opCode = frame.getInt(frame.readerIndex() + 12);

		// logger.info("Mongo message opcode " + opCode + " received");

		Message message = null;
		if (opCode == Message.OP_DELETE) {
			message = new OpDelete();
		} else if (opCode == Message.OP_GET_MORE) {
			message = new OpGetMore();
		} else if (opCode == Message.OP_INSERT) {
			message = new OpInsert();
		} else if (opCode == Message.OP_KILL_CURSORS) {
			message = new OpKillCursors();
		} else if (opCode == Message.OP_MSG) {
			message = new OpMsg();
		} else if (opCode == Message.OP_QUERY) {
			message = new OpQuery();
		} else if (opCode == Message.OP_REPLY) {
			message = new OpReply();
		} else if (opCode == Message.OP_UPDATE) {
			message = new OpUpdate();
		}

		if (message != null) {
			message.decode(frame);
		} else {
			logger.info("Mongo unrecongnized message opcode " + opCode
					+ " received");
		}

		// logger.info(message);

		return message;
	}

	static MongoMessageDecoder _instance = new MongoMessageDecoder();

	public static Message decode(ChannelBuffer buf) throws Exception {
		return (Message) _instance.decode(null, null, buf.duplicate());
	}
}
