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
