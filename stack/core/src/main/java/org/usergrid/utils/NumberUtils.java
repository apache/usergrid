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
package org.usergrid.utils;

public class NumberUtils {

	/**
	 * @param obj
	 * @return
	 */
	public static long longValue(Object obj) {
		if (obj instanceof Number) {
			return ((Number) obj).longValue();
		}
		throw new NumberFormatException("Value object is not a long");
	}

	/**
	 * @param obj
	 * @return
	 */
	public static boolean isLong(Object obj) {
		return obj instanceof Long;
	}

	/**
	 * @param obj
	 * @return
	 */
	public static boolean isLongOrNull(Object obj) {
		if (obj == null) {
			return true;
		}
		return obj instanceof Long;
	}

	public static int sign(int i) {
		if (i < 0) {
			return -1;
		}
		if (i > 0) {
			return 1;
		}
		return 0;
	}

	public static long roundLong(long l, long r) {
		return (l / r) * r;
	}

}
