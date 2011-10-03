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

	@Override
	public String toString() {
		return "OpKillCursors [numberOfCursorIDs=" + numberOfCursorIDs
				+ ", cursorIDs=" + cursorIDs + "]";
	}

}
