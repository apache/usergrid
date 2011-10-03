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
