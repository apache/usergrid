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
