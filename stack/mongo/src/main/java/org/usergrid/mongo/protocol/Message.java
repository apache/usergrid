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
