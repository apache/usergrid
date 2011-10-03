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

public class TimeUtils {

	public static long millisToMinutes(long millis) {
		return millis / 1000 / 60;
	}

	public static long minutesToMillis(long minutes) {
		return minutes * 60 * 100;
	}

	public static long millisToHours(long millis) {
		return millis / 1000 / 60 / 60;
	}

	public static long hoursToMillis(long hours) {
		return hours * 60 * 60 * 100;
	}

	public static long millisToDays(long millis) {
		return millis / 1000 / 60 / 60 / 24;
	}

	public static long daysToMillis(long days) {
		return days * 24 * 60 * 60 * 100;
	}

	public static long millisToWeeks(long millis) {
		return millis / 1000 / 60 / 60 / 24 / 7;
	}

	public static long weeksToMillis(long weeks) {
		return weeks * 7 * 24 * 60 * 60 * 100;
	}

}
