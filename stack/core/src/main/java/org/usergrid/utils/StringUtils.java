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

import static org.usergrid.utils.ConversionUtils.string;

import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author edanuff
 * 
 */
public class StringUtils extends org.apache.commons.lang.StringUtils {

	private static final Logger logger = LoggerFactory
			.getLogger(StringUtils.class);

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

	public static String toStringFormat(Object obj, String format) {
		if (obj != null) {
			if (format != null) {
				if (obj.getClass().isArray()) {
					return String.format(format,
							Arrays.toString((Object[]) obj));
				}
				return String.format(format, string(obj));
			} else {
				return string(obj);
			}
		}
		return "";
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
		try {
			return IOUtils.toString(StringUtils.class
					.getResourceAsStream(filePath));
		} catch (Exception e) {
			logger.error("Error getting file from classpath: " + filePath, e);
		}
		return null;
	}

}
