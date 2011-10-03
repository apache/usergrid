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
package org.usergrid.mongo.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.usergrid.mongo.utils.BSONUtils;

public class OpDelete extends OpCrud {

	int flags;
	BSONObject selector;

	public OpDelete() {
		opCode = OP_DELETE;
	}

	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public BSONObject getSelector() {
		return selector;
	}

	public void setSelector(BSONObject selector) {
		this.selector = selector;
	}

	public void setSelector(Map<?, ?> map) {
		selector = new BasicBSONObject();
		selector.putAll(map);
	}

	@Override
	public void decode(ChannelBuffer buffer) throws IOException {
		super.decode(buffer);
		buffer.readInt();
		fullCollectionName = readCString(buffer);
		flags = buffer.readInt();
		selector = BSONUtils.decoder().readObject(
				new ChannelBufferInputStream(buffer));
	}

	@Override
	public ChannelBuffer encode(ChannelBuffer buffer) {
		int l = 24; // 6 ints * 4 bytes

		ByteBuffer fullCollectionNameBytes = getCString(fullCollectionName);
		l += fullCollectionNameBytes.capacity();

		ByteBuffer selectorBytes = encodeDocument(selector);
		l += selectorBytes.capacity();

		messageLength = l;

		buffer = super.encode(buffer);

		buffer.writeInt(0);

		buffer.writeBytes(fullCollectionNameBytes);

		buffer.writeInt(flags);

		buffer.writeBytes(selectorBytes);

		return buffer;
	}

	@Override
	public String toString() {
		return "OpDelete [fullCollectionName=" + fullCollectionName
				+ ", flags=" + flags + ", selector=" + selector + "]";
	}

}
