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
		return "OpReply [responseFlags=" + responseFlags + ", cursorID="
				+ cursorID + ", startingFrom=" + startingFrom
				+ ", numberReturned=" + numberReturned + ", documents="
				+ documents + "]";
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
