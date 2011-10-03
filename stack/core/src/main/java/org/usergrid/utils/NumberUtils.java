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
