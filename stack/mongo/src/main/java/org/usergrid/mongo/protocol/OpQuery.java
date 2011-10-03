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

public class OpQuery extends OpCrud {

	int flags;
	int numberToSkip;
	int numberToReturn;
	BSONObject query;
	BSONObject returnFieldSelector;

	public OpQuery() {
		opCode = OP_QUERY;
	}

	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public int getNumberToSkip() {
		return numberToSkip;
	}

	public void setNumberToSkip(int numberToSkip) {
		this.numberToSkip = numberToSkip;
	}

	public int getNumberToReturn() {
		return numberToReturn;
	}

	public void setNumberToReturn(int numberToReturn) {
		this.numberToReturn = numberToReturn;
	}

	public BSONObject getQuery() {
		return query;
	}

	public void setQuery(BSONObject query) {
		this.query = query;
	}

	public void setQuery(Map<?, ?> map) {
		query = new BasicBSONObject();
		query.putAll(map);
	}

	public BSONObject getReturnFieldSelector() {
		return returnFieldSelector;
	}

	public void setReturnFieldSelector(BSONObject returnFieldSelector) {
		this.returnFieldSelector = returnFieldSelector;
	}

	public void setReturnFieldSelector(Map<?, ?> map) {
		returnFieldSelector = new BasicBSONObject();
		returnFieldSelector.putAll(map);
	}

	@Override
	public void decode(ChannelBuffer buffer) throws IOException {
		super.decode(buffer);
		flags = buffer.readInt();
		fullCollectionName = readCString(buffer);
		numberToSkip = buffer.readInt();
		numberToReturn = buffer.readInt();
		query = BSONUtils.decoder().readObject(
				new ChannelBufferInputStream(buffer));
		if (buffer.readable()) {
			returnFieldSelector = BSONUtils.decoder().readObject(
					new ChannelBufferInputStream(buffer));
		}
	}

	@Override
	public ChannelBuffer encode(ChannelBuffer buffer) {
		int l = 28; // 7 ints * 4 bytes

		ByteBuffer fullCollectionNameBytes = getCString(fullCollectionName);
		l += fullCollectionNameBytes.capacity();

		ByteBuffer queryBytes = encodeDocument(query);
		l += queryBytes.capacity();

		ByteBuffer returnFieldSelectorBytes = encodeDocument(returnFieldSelector);
		l += returnFieldSelectorBytes.capacity();

		messageLength = l;

		buffer = super.encode(buffer);

		buffer.writeInt(flags);

		buffer.writeBytes(fullCollectionNameBytes);

		buffer.writeInt(numberToSkip);
		buffer.writeInt(numberToReturn);

		buffer.writeBytes(queryBytes);

		buffer.writeBytes(returnFieldSelectorBytes);

		return buffer;
	}

	@Override
	public String toString() {
		return "OpQuery [flags=" + flags + ", fullCollectionName="
				+ fullCollectionName + ", numberToSkip=" + numberToSkip
				+ ", numberToReturn=" + numberToReturn + ", query=" + query
				+ ", returnFieldSelector=" + returnFieldSelector + "]";
	}

}
