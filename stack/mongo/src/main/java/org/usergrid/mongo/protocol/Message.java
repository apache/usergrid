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
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.usergrid.mongo.utils.BSONUtils;
import org.usergrid.utils.StringUtils;

public class Message {

	public static final int OP_REPLY = 1; // Reply to a client request
	public static final int OP_MSG = 1000; // generic msg command
	public static final int OP_UPDATE = 2001; // update document
	public static final int OP_INSERT = 2002; // insert new document
	public static final int RESERVED = 2003; // formerly used for OP_GET_BY_OID
	public static final int OP_QUERY = 2004; // query a collection
	public static final int OP_GET_MORE = 2005; // Get more data from a query
	public static final int OP_DELETE = 2006; // Delete documents
	public static final int OP_KILL_CURSORS = 2007; // Client done with cursor

	int messageLength;
	int requestID;
	int responseTo;
	int opCode;

	public Message() {

	}

	public int getMessageLength() {
		return messageLength;
	}

	public void setMessageLength(int messageLength) {
		this.messageLength = messageLength;
	}

	public int getRequestID() {
		return requestID;
	}

	public void setRequestID(int requestID) {
		this.requestID = requestID;
	}

	public int getResponseTo() {
		return responseTo;
	}

	public void setResponseTo(int responseTo) {
		this.responseTo = responseTo;
	}

	public int getOpCode() {
		return opCode;
	}

	public void setOpCode(int opCode) {
		this.opCode = opCode;
	}

	public void decode(ChannelBuffer buffer) throws IOException {
		messageLength = buffer.readInt();
		requestID = buffer.readInt();
		responseTo = buffer.readInt();
		opCode = buffer.readInt();
	}

	public ChannelBuffer encode(ChannelBuffer buffer) {
		if (buffer == null) {
			buffer = ChannelBuffers.buffer(ByteOrder.LITTLE_ENDIAN,
					messageLength);
		}
		buffer.writeInt(messageLength);
		buffer.writeInt(requestID);
		buffer.writeInt(responseTo);
		buffer.writeInt(opCode);
		return buffer;
	}

	public String readCString(ChannelBuffer buffer) {
		int i = buffer.bytesBefore((byte) 0);
		if (i < 0) {
			return null;
		}
		String s = buffer.toString(buffer.readerIndex(), i,
				Charset.forName("UTF-8"));
		buffer.skipBytes(i + 1);
		return s;
	}

	public void writeCString(String str, ChannelBuffer buffer) {
		if (str != null) {
			buffer.writeBytes(str.getBytes(Charset.forName("UTF-8")));
		}
		buffer.writeByte(0);
	}

	public ByteBuffer getCString(String str) {
		byte[] bytes = new byte[0];
		if (str != null) {
			bytes = str.getBytes(Charset.forName("UTF-8"));
		}
		ByteBuffer buffer = ByteBuffer.allocate(bytes.length + 1);
		buffer.put(bytes);
		buffer.rewind();
		return buffer;
	}

	public static List<ByteBuffer> encodeDocuments(List<BSONObject> documents) {
		List<ByteBuffer> encodedDocuments = new ArrayList<ByteBuffer>();
		if (documents != null) {
			for (BSONObject d : documents) {
				byte[] encoded = BSONUtils.encoder().encode(d);
				encodedDocuments.add(ByteBuffer.wrap(encoded));
			}
		}
		return encodedDocuments;
	}

	public static int buffersSize(List<ByteBuffer> buffers) {
		int l = 0;
		for (ByteBuffer b : buffers) {
			l += b.capacity();
		}
		return l;
	}

	public static ByteBuffer encodeDocument(BSONObject document) {
		if (document == null) {
			return ByteBuffer.allocate(0);
		}
		return ByteBuffer.wrap(BSONUtils.encoder().encode(document));
	}

	public static String getDatabaseName(String fullCollectionName) {
		return StringUtils
				.stringOrSubstringBeforeFirst(fullCollectionName, '.');
	}

	public static String getCollectionName(String fullCollectionName) {
		return StringUtils.stringOrSubstringAfterFirst(fullCollectionName, '.');
	}

}
