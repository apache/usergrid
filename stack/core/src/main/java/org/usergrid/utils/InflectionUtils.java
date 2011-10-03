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

public class InflectionUtils {

	public static String pluralize(Object word) {
		return Inflector.INSTANCE.pluralize(word);
	}

	public static String singularize(Object word) {
		return Inflector.INSTANCE.singularize(word);
	}

	public static boolean isPlural(Object word) {
		return Inflector.INSTANCE.isPlural(word);
	}

	public static boolean isSingular(Object word) {
		return Inflector.INSTANCE.isSingular(word);
	}

	public static String underscore(String s) {
		return Inflector.INSTANCE.underscore(s);
	}

	public static String camelCase(String lowerCaseAndUnderscoredWord,
			boolean uppercaseFirstLetter, char... delimiterChars) {
		return Inflector.INSTANCE.camelCase(lowerCaseAndUnderscoredWord,
				uppercaseFirstLetter, delimiterChars);
	}

}
