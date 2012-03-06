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
