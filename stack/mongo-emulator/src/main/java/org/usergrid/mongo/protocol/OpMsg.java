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

public class OpMsg extends Message {

	String message;

	public OpMsg() {
		opCode = OP_MSG;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public void decode(ChannelBuffer buffer) throws IOException {
		super.decode(buffer);
		message = readCString(buffer);
	}

	@Override
	public ChannelBuffer encode(ChannelBuffer buffer) {
		int l = 16; // 4 ints * 4 bytes

		ByteBuffer messageBytes = getCString(message);
		l += messageBytes.capacity();

		messageLength = l;

		buffer = super.encode(buffer);

		buffer.writeBytes(messageBytes);

		return buffer;
	}

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "OpMsg [message=" + message + ", messageLength=" + messageLength
                + ", requestID=" + requestID + ", responseTo=" + responseTo
                + ", opCode=" + opCode + "]";
    }

	

}
