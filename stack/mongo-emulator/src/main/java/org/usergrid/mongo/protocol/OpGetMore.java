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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.usergrid.mongo.MongoChannelHandler;

public class OpGetMore extends OpCrud {

	int numberToReturn;
	long cursorID;

	public OpGetMore() {
		opCode = OP_GET_MORE;
	}

	public int getNumberToReturn() {
		return numberToReturn;
	}

	public void setNumberToReturn(int numberToReturn) {
		this.numberToReturn = numberToReturn;
	}

	public long getCursorID() {
		return cursorID;
	}

	public void setCursorID(long cursorID) {
		this.cursorID = cursorID;
	}

	@Override
	public void decode(ChannelBuffer buffer) throws IOException {
		super.decode(buffer);

		buffer.readInt();
		fullCollectionName = readCString(buffer);
		numberToReturn = buffer.readInt();
		cursorID = buffer.readLong();
	}

	@Override
	public ChannelBuffer encode(ChannelBuffer buffer) {
		int l = 32; // 8 ints * 4 bytes

		ByteBuffer fullCollectionNameBytes = getCString(fullCollectionName);
		l += fullCollectionNameBytes.capacity();

		messageLength = l;

		buffer = super.encode(buffer);

		buffer.writeInt(0);

		buffer.writeBytes(fullCollectionNameBytes);

		buffer.writeInt(numberToReturn);

		buffer.writeLong(cursorID);

		return buffer;
	}

	
    /* (non-Javadoc)
     * @see org.usergrid.mongo.protocol.OpCrud#doOp(org.usergrid.mongo.MongoChannelHandler, org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
     */
    @Override
    public OpReply doOp(MongoChannelHandler handler, ChannelHandlerContext ctx,
            MessageEvent messageEvent) {
        return new OpReply(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "OpGetMore [numberToReturn=" + numberToReturn + ", cursorID="
                + cursorID + ", fullCollectionName=" + fullCollectionName
                + ", messageLength=" + messageLength + ", requestID="
                + requestID + ", responseTo=" + responseTo + ", opCode="
                + opCode + "]";
    }

}
