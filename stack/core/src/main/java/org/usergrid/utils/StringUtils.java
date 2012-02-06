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

import static org.usergrid.utils.ConversionUtils.string;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author edanuff
 * 
 */
public class StringUtils extends org.apache.commons.lang.StringUtils {

	public static String[] lowerCaseArray(String[] strings) {
		if (strings != null) {
			for (int i = 0; i < strings.length; i++) {
				strings[i] = strings[i].toLowerCase();
			}
		}
		return strings;
	}

	public static String capitalizeDelimiter(String str, char... delims) {
		if (str == null) {
			return null;
		}
		StringBuilder builder = new StringBuilder(str.length());
		boolean cap_next = true;
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (cap_next) {
				c = Character.toUpperCase(c);
			} else {
				c = Character.toLowerCase(c);
			}
			builder.append(c);
			cap_next = false;
			for (char delim : delims) {
				if (c == delim) {
					cap_next = true;
					break;
				}
			}
		}
		return builder.toString();
	}

	/**
	 * @param str
	 * @return
	 */
	public static String capitalizeUnderscore(String str) {
		if (str == null) {
			return null;
		}
		return capitalizeDelimiter(str, '_');
	}

	/**
	 * @param obj
	 * @return
	 */
	public static Object lower(Object obj) {
		if (!(obj instanceof String)) {
			return obj;
		}
		return ((String) obj).toLowerCase();
	}

	public static String stringOrSubstringAfterLast(String str, char c) {
		if (str == null) {
			return null;
		}
		int i = str.lastIndexOf(c);
		if (i != -1) {
			return str.substring(i + 1);
		}
		return str;
	}

	public static String stringOrSubstringBeforeLast(String str, char c) {
		if (str == null) {
			return null;
		}
		int i = str.lastIndexOf(c);
		if (i != -1) {
			return str.substring(0, i);
		}
		return str;
	}

	public static String stringOrSubstringBeforeFirst(String str, char c) {
		if (str == null) {
			return null;
		}
		int i = str.indexOf(c);
		if (i != -1) {
			return str.substring(0, i);
		}
		return str;
	}

	public static String stringOrSubstringAfterFirst(String str, char c) {
		if (str == null) {
			return null;
		}
		int i = str.indexOf(c);
		if (i != -1) {
			return str.substring(i + 1);
		}
		return str;
	}

	public static String compactWhitespace(String str) {
		if (str == null) {
			return null;
		}
		boolean prev_ws = false;
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (Character.isWhitespace(c)) {
				if (!prev_ws) {
					builder.append(' ');
				}
				prev_ws = true;
			} else {
				prev_ws = false;
				builder.append(c);
			}
		}
		return builder.toString().trim();
	}

	/**
	 * @param source
	 * @param find
	 * @param replace
	 * @return new string with replace applied
	 */
	public static String replaceAll(String source, String find, String replace) {
		if (source == null) {
			return null;
		}
		while (true) {
			String old = source;
			source = source.replaceAll(find, replace);
			if (source.equals(old)) {
				return source;
			}
		}
	}

	/**
	 * @param obj
	 * @return
	 */
	public static String toString(Object obj) {
		return string(obj);
	}

	/**
	 * @param obj
	 * @return
	 */
	public static String ifString(Object obj) {
		if (obj instanceof String) {
			return (String) obj;
		}
		return null;
	}

	/**
	 * @param obj
	 * @return
	 */
	public static Object toLowerIfString(Object obj) {
		if (obj instanceof String) {
			return ((String) obj).toLowerCase();
		}
		return obj;
	}

	/**
	 * @param obj
	 * @return
	 */
	public static boolean isString(Object obj) {
		return obj instanceof String;
	}

	/**
	 * @param obj
	 * @return
	 */
	public static boolean isStringOrNull(Object obj) {
		if (obj == null) {
			return true;
		}
		return obj instanceof String;
	}

	public static String readClasspathFileAsString(String filePath) {
		StringBuffer fileData = new StringBuffer(1000);
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				StringUtils.class.getResourceAsStream(filePath)));
		char[] buf = new char[1024];
		int numRead = 0;
		try {
			while ((numRead = reader.read(buf)) != -1) {
				String readData = String.valueOf(buf, 0, numRead);
				fileData.append(readData);
				buf = new char[1024];
			}
		} catch (Exception e) {
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
			}
		}
		return fileData.toString();
	}

}
