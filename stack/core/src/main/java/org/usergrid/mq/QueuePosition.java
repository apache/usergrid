/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Core.
 * 
 * Usergrid Core is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Usergrid Core is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Usergrid Core. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.usergrid.mq;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum QueuePosition {
	START("start"), END("end"), LAST("last"), CONSUMER("consumer");

	private final String shortName;

	QueuePosition(String shortName) {
		this.shortName = shortName;
	}

	static Map<String, QueuePosition> nameMap = new ConcurrentHashMap<String, QueuePosition>();

	static {
		for (QueuePosition op : EnumSet.allOf(QueuePosition.class)) {
			if (op.shortName != null) {
				nameMap.put(op.shortName, op);
			}
		}
	}

	public static QueuePosition find(String s) {
		if (s == null) {
			return null;
		}
		return nameMap.get(s);
	}

	@Override
	public String toString() {
		return shortName;
	}
}
