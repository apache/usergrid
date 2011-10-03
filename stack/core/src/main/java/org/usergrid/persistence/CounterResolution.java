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
package org.usergrid.persistence;

public enum CounterResolution {
	ALL(0), MINUTE(1), FIVE_MINUTES(5), HALF_HOUR(30), HOUR(60), SIX_HOUR(
			60 * 6), HALF_DAY(60 * 12), DAY(60 * 24), WEEK(60 * 24 * 7), MONTH(
			60 * 24 * (365 / 12));

	private final long interval;

	CounterResolution(long minutes) {
		interval = minutes * 60 * 1000;
	}

	public long interval() {
		return interval;
	}

	public long round(long timestamp) {
		if (interval == 0) {
			return 1;
		}
		return (timestamp / interval) * interval;
	}

	public long next(long timestamp) {
		return round(timestamp) + interval;
	}

	public static CounterResolution fromOrdinal(int i) {
		if ((i < 0) || (i >= CounterResolution.values().length)) {
			throw new IndexOutOfBoundsException("Invalid ordinal");
		}
		return CounterResolution.values()[i];
	}

	public static CounterResolution fromMinutes(int m) {
		m = m * 60 * 1000;
		for (int i = CounterResolution.values().length - 1; i >= 0; i--) {
			if (CounterResolution.values()[i].interval <= m) {
				return CounterResolution.values()[i];
			}
		}
		return ALL;
	}

	public static CounterResolution fromString(String s) {
		if (s == null) {
			return ALL;
		}
		try {
			return CounterResolution.valueOf(s.toUpperCase());
		} catch (IllegalArgumentException e) {
		}
		try {
			return fromMinutes(Integer.valueOf(s));
		} catch (NumberFormatException e) {
		}
		return ALL;
	}
}
