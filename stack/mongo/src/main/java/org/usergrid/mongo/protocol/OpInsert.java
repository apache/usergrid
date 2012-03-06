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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.usergrid.mongo.utils.BSONUtils;

public class OpInsert extends OpCrud {

	int flags;
	List<BSONObject> documents = new ArrayList<BSONObject>();

	public OpInsert() {
		opCode = OP_INSERT;
	}

	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public List<BSONObject> getDocuments() {
		return documents;
	}

	public void setDocuments(List<BSONObject> documents) {
		if (documents == null) {
			documents = new ArrayList<BSONObject>();
		}
		this.documents = documents;
	}

	public void addDocument(BSONObject document) {
		documents.add(document);
	}

	public void addDocument(Map<?, ?> map) {
		BSONObject b = new BasicBSONObject();
		b.putAll(map);
		documents.add(b);
	}

	@Override
	public void decode(ChannelBuffer buffer) throws IOException {
		super.decode(buffer);

		flags = buffer.readInt();
		fullCollectionName = readCString(buffer);

		while (buffer.readable()) {
			documents.add(BSONUtils.decoder().readObject(
					new ChannelBufferInputStream(buffer)));
		}
	}

	@Override
	public ChannelBuffer encode(ChannelBuffer buffer) {
		int l = 20; // 5 ints * 4 bytes

		ByteBuffer fullCollectionNameBytes = getCString(fullCollectionName);
		l += fullCollectionNameBytes.capacity();

		List<ByteBuffer> encodedDocuments = encodeDocuments(documents);
		l += buffersSize(encodedDocuments);

		messageLength = l;

		buffer = super.encode(buffer);

		buffer.writeInt(flags);

		buffer.writeBytes(fullCollectionNameBytes);

		for (ByteBuffer d : encodedDocuments) {
			buffer.writeBytes(d);
		}

		return buffer;
	}

	@Override
	public String toString() {
		return "OpInsert [flags=" + flags + ", fullCollectionName="
				+ fullCollectionName + ", documents=" + documents + "]";
	}

}
