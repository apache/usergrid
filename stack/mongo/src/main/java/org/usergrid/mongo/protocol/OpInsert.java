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
