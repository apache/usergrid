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
package org.usergrid.mongo.testproxy;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoMessageFrame extends FrameDecoder {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory
			.getLogger(MongoMessageFrame.class);

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel,
			ChannelBuffer buf) throws Exception {

		if (buf.readableBytes() < 4) {
			return null;
		}

		// logger.info("Mongo message decoding...");

		int length = buf.getInt(buf.readerIndex());

		if (length < 0) {
			return null;
		}

		if (buf.readableBytes() < length) {
			return null;
		}

		ChannelBuffer frame = buf.readSlice(length);
		return frame;
	}

}
