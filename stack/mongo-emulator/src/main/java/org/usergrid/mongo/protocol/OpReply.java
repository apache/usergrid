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

public class OpReply extends Message {

	int responseFlags = 8;
	long cursorID;
	int startingFrom;
	int numberReturned;
	List<BSONObject> documents = new ArrayList<BSONObject>();

	public OpReply() {
		opCode = OP_REPLY;
	}

	public OpReply(Message message) {
		opCode = OP_REPLY;
		responseTo = message.getRequestID();
	}

	public int getResponseFlags() {
		return responseFlags;
	}

	public void setResponseFlags(int responseFlags) {
		this.responseFlags = responseFlags;
	}

	public long getCursorID() {
		return cursorID;
	}

	public void setCursorID(long cursorID) {
		this.cursorID = cursorID;
	}

	public int getStartingFrom() {
		return startingFrom;
	}

	public void setStartingFrom(int startingFrom) {
		this.startingFrom = startingFrom;
	}

	public int getNumberReturned() {
		return numberReturned;
	}

	public void setNumberReturned(int numberReturned) {
		this.numberReturned = numberReturned;
	}

	public List<BSONObject> getDocuments() {
		return documents;
	}

	public void setDocuments(List<BSONObject> documents) {
		if (documents == null) {
			documents = new ArrayList<BSONObject>();
		}
		this.documents = documents;
		numberReturned = documents.size();
	}

	public void addDocument(BSONObject document) {
		documents.add(document);
		numberReturned = documents.size();
	}

	public void addDocument(Map<?, ?> map) {
		BSONObject b = new BasicBSONObject();
		b.putAll(map);
		documents.add(b);
		numberReturned = documents.size();
	}

	@Override
	public void decode(ChannelBuffer buffer) throws IOException {
		super.decode(buffer);

		responseFlags = buffer.readInt();
		cursorID = buffer.readLong();
		startingFrom = buffer.readInt();
		numberReturned = buffer.readInt();

		while (buffer.readable()) {
			documents.add(BSONUtils.decoder().readObject(
					new ChannelBufferInputStream(buffer)));
		}
	}

	@Override
	public ChannelBuffer encode(ChannelBuffer buffer) {
		int l = 36; // (9 ints * 4 bytes)

		List<ByteBuffer> encodedDocuments = encodeDocuments(documents);
		l += buffersSize(encodedDocuments);
		numberReturned = encodedDocuments.size();

		messageLength = l;

		buffer = super.encode(buffer);

		buffer.writeInt(responseFlags);
		buffer.writeLong(cursorID);
		buffer.writeInt(startingFrom);
		buffer.writeInt(numberReturned);

		for (ByteBuffer d : encodedDocuments) {
			buffer.writeBytes(d);
		}

		return buffer;
	}

	@Override
	public String toString() {
		String docs_str = null;
		try {
			docs_str = documents.toString();
		} catch (Exception e) {
			docs_str = "error(" + e.getMessage() + ")";
		}
		return "OpReply [responseFlags=" + responseFlags + ", cursorID="
				+ cursorID + ", startingFrom=" + startingFrom
				+ ", numberReturned=" + numberReturned + ", documents="
				+ docs_str + "]";
	}

	public static OpReply errorReply(String message) {
		OpReply reply = new OpReply();
		// reply.responseFlags = 1;
		BSONObject b = new BasicBSONObject();
		b.put("$err", message);
		b.put("ok", 0.0);
		reply.documents.add(b);
		return reply;
	}

}
