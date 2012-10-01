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
import java.util.ArrayList;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;

public class OpKillCursors extends Message {

	int numberOfCursorIDs;
	List<Long> cursorIDs = new ArrayList<Long>();

	public OpKillCursors() {
		opCode = OP_KILL_CURSORS;
	}

	public int getNumberOfCursorIDs() {
		return numberOfCursorIDs;
	}

	public void setNumberOfCursorIDs(int numberOfCursorIDs) {
		this.numberOfCursorIDs = numberOfCursorIDs;
	}

	public List<Long> getCursorIDs() {
		return cursorIDs;
	}

	public void setCursorIDs(List<Long> cursorIDs) {
		if (cursorIDs == null) {
			cursorIDs = new ArrayList<Long>();
		}
		this.cursorIDs = cursorIDs;
		numberOfCursorIDs = cursorIDs.size();
	}

	public void addCursorIDs(long cursorID) {
		cursorIDs.add(cursorID);
		numberOfCursorIDs = cursorIDs.size();
	}

	@Override
	public void decode(ChannelBuffer buffer) throws IOException {
		super.decode(buffer);

		buffer.readInt();
		numberOfCursorIDs = buffer.readInt();
		while (buffer.readable()) {
			cursorIDs.add(buffer.readLong());
		}
	}

	@Override
	public ChannelBuffer encode(ChannelBuffer buffer) {
		int l = 24; // (6 ints * 4 bytes)

		numberOfCursorIDs = 0;
		if (cursorIDs != null) {
			numberOfCursorIDs = cursorIDs.size();
			l += numberOfCursorIDs * 8;
		}
		messageLength = l;

		buffer = super.encode(buffer);

		buffer.writeInt(0);

		buffer.writeInt(numberOfCursorIDs);

		if (cursorIDs != null) {
			for (Long cursorID : cursorIDs) {
				buffer.writeLong(cursorID);
			}
		}

		return buffer;
	}

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "OpKillCursors [numberOfCursorIDs=" + numberOfCursorIDs
                + ", cursorIDs=" + cursorIDs + ", messageLength="
                + messageLength + ", requestID=" + requestID + ", responseTo="
                + responseTo + ", opCode=" + opCode + "]";
    }

	

}
