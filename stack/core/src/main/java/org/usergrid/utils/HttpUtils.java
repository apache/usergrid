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

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

public class HttpUtils {

	public static Map<String, List<String>> parseQueryString(String queryString) {
		Map<String, List<String>> mapOfLists = new HashMap<String, List<String>>();
		if ((queryString == null) || (queryString.length() == 0)) {
			return mapOfLists;
		}
		List<NameValuePair> list = URLEncodedUtils.parse(
				URI.create("http://localhost/?" + queryString), "UTF-8");
		for (NameValuePair pair : list) {
			List<String> values = mapOfLists.get(pair.getName());
			if (values == null) {
				values = new ArrayList<String>();
				mapOfLists.put(pair.getName(), values);
			}
			if (pair.getValue() != null) {
				values.add(pair.getValue());
			}
		}

		return mapOfLists;
	}

}
